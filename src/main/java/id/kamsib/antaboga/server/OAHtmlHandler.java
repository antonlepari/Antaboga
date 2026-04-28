package id.kamsib.antaboga.server;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import id.kamsib.antaboga.security.InputValidator;
import id.kamsib.antaboga.security.PasswordHasher;
import id.kamsib.antaboga.security.SessionManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Map;

/**
 * Handler for /OA_HTML/ paths.
 * Serves JSP-like template pages rendered by the Java backend.
 * 
 * Mimics Oracle E-Business Suite URL patterns:
 *   /OA_HTML/AppsLocalLogin.jsp   — Login page
 *   /OA_HTML/AppsHome.jsp         — Home navigator
 *   /OA_HTML/RF.jsp               — Module launcher (Responsibility Function)
 *   /OA_HTML/OA.jsp               — Module page renderer
 */
public class OAHtmlHandler implements HttpHandler {

    private final Connection db;
    private final SessionManager sessions;

    public OAHtmlHandler(Connection db, SessionManager sessions) {
        this.db = db;
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // Extract the JSP filename from path
        String page = path.replace("/OA_HTML/", "").replace("/OA_HTML", "");
        if (page.isEmpty() || page.equals("/")) {
            page = "AppsLocalLogin.jsp";
        }

        try {
            switch (page) {
                case "AppsLocalLogin.jsp":
                    handleLogin(exchange, method);
                    break;
                case "AppsHome.jsp":
                    handleHome(exchange);
                    break;
                case "RF.jsp":
                    handleResponsibility(exchange);
                    break;
                case "OA.jsp":
                    handleModulePage(exchange);
                    break;
                case "runforms.jsp":
                    handleRunForms(exchange);
                    break;
                case "AppsLogout.jsp":
                    handleLogout(exchange);
                    break;
                default:
                    sendPage(exchange, 404, "404.jsp", TemplateEngine.newContext());
            }
        } catch (Exception e) {
            System.err.println("[OA_HTML ERROR] " + e.getMessage());
            Map<String, String> ctx = TemplateEngine.newContext();
            ctx.put("errorMessage", "Terjadi kesalahan internal server.");
            try {
                sendPage(exchange, 500, "500.jsp", ctx);
            } catch (Exception ex) {
                exchange.sendResponseHeaders(500, -1);
            }
        }
    }

    /**
     * /OA_HTML/AppsLocalLogin.jsp
     * GET: Show login form
     * POST: Process login
     */
    private void handleLogin(HttpExchange exchange, String method) throws Exception {
        // Rate limiting for JSP login (prevent brute force)
        String ip = HttpUtil.getClientIp(exchange);
        if (!sessions.checkRateLimit(ip)) {
            Map<String, String> ctx = TemplateEngine.newContext();
            ctx.put("errorMessage", "<div class=\"error-box\">Terlalu banyak percobaan login. Coba lagi dalam 1 menit.</div>");
            ctx.put("lastUsername", "");
            sendPage(exchange, 429, "AppsLocalLogin.jsp", ctx);
            return;
        }

        Map<String, String> ctx = TemplateEngine.newContext();

        if ("POST".equals(method)) {
            // Parse form data
            String body = new String(readBody(exchange), StandardCharsets.UTF_8);
            Map<String, String> params = HttpUtil.parseQuery(body);

            String username = InputValidator.clean(params.getOrDefault("usernameField", ""));
            String password = params.getOrDefault("passwordField", "");

            if (username.isEmpty() || password.isEmpty()) {
                ctx.put("errorMessage", "<div class=\"error-box\">Username dan password harus diisi.</div>");
                ctx.put("lastUsername", InputValidator.sanitize(username));
                sendPage(exchange, 200, "AppsLocalLogin.jsp", ctx);
                return;
            }

            // Authenticate
            PreparedStatement ps = db.prepareStatement(
                "SELECT id, password_hash, full_name, role, is_active FROM users WHERE username = ?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                if (!rs.getBoolean("is_active")) {
                    ctx.put("errorMessage", "<div class=\"error-box\">Akun tidak aktif. Hubungi administrator.</div>");
                    ctx.put("lastUsername", InputValidator.sanitize(username));
                    sendPage(exchange, 200, "AppsLocalLogin.jsp", ctx);
                    rs.close(); ps.close();
                    return;
                }

                if (PasswordHasher.verify(password, rs.getString("password_hash"))) {
                    int userId = rs.getInt("id");
                    String sessionId = sessions.createSession(userId);

                    // Audit
                    logAction(userId, "JSP_LOGIN", "user", userId,
                        "Login via AppsLocalLogin.jsp dari " + HttpUtil.getClientIp(exchange));

                    HttpUtil.setSessionCookie(exchange, sessionId, 3600);
                    rs.close(); ps.close();

                    // Redirect to home
                    exchange.getResponseHeaders().set("Location", "/OA_HTML/AppsHome.jsp");
                    exchange.sendResponseHeaders(302, -1);
                    return;
                }
            }

            rs.close(); ps.close();
            ctx.put("errorMessage", "<div class=\"error-box\">Username atau password tidak valid.</div>");
            ctx.put("lastUsername", InputValidator.sanitize(username));
            sendPage(exchange, 200, "AppsLocalLogin.jsp", ctx);

        } else {
            // GET: show login form
            ctx.put("lastUsername", "");
            sendPage(exchange, 200, "AppsLocalLogin.jsp", ctx);
        }
    }

    /**
     * /OA_HTML/AppsHome.jsp — Home navigator (requires auth)
     */
    private void handleHome(HttpExchange exchange) throws Exception {
        int userId = requireAuth(exchange);
        if (userId < 0) return;

        Map<String, String> ctx = TemplateEngine.newContext();

        // Load user info
        PreparedStatement ps = db.prepareStatement("SELECT * FROM users WHERE id = ?");
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            ctx.put("userFullName", rs.getString("full_name"));
            ctx.put("userRole", rs.getString("role"));
            ctx.put("userEmail", rs.getString("email") != null ? rs.getString("email") : "");
        }
        rs.close(); ps.close();

        // Load stats
        ctx.put("totalStudents", String.valueOf(countTable("students")));
        ctx.put("totalTeachers", String.valueOf(countTable("teachers")));
        ctx.put("totalClasses", String.valueOf(countTable("classes")));
        ctx.put("totalSubjects", String.valueOf(countTable("subjects")));
        ctx.put("unpaidInvoices", String.valueOf(countWhere("invoices", "status = 'unpaid'")));

        sendPage(exchange, 200, "AppsHome.jsp", ctx);
    }

    /**
     * /OA_HTML/RF.jsp?module=xxx&action=xxx — Responsibility Function (module launcher)
     * Supports: view (default), add (show form), save (POST), delete
     */
    private void handleResponsibility(HttpExchange exchange) throws Exception {
        int userId = requireAuth(exchange);
        if (userId < 0) return;

        String method = exchange.getRequestMethod();
        Map<String, String> params = HttpUtil.parseQuery(exchange.getRequestURI().getQuery());
        String funcId = params.getOrDefault("function_id", "");
        String module = params.getOrDefault("module", "");
        // function_id takes priority for Oracle EBS compatibility
        if (!funcId.isEmpty() && module.isEmpty()) {
            module = mapFuncToModule(funcId);
        }
        if (module.isEmpty()) module = "dashboard";
        String action = params.getOrDefault("action", "view");

        // Handle POST (form save)
        if ("POST".equals(method)) {
            String body = new String(readBody(exchange), java.nio.charset.StandardCharsets.UTF_8);
            Map<String, String> form = HttpUtil.parseQuery(body);
            handleModuleSave(module, form, userId);
            exchange.getResponseHeaders().set("Location",
                "/OA_HTML/RF.jsp?module=" + module + "&msg=success");
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        // Handle delete
        if ("delete".equals(action) && params.containsKey("id")) {
            handleModuleDelete(module, Integer.parseInt(params.get("id")), userId);
            exchange.getResponseHeaders().set("Location",
                "/OA_HTML/RF.jsp?module=" + module + "&msg=deleted");
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        Map<String, String> ctx = TemplateEngine.newContext();
        ctx.put("currentModule", module);
        ctx.put("moduleTitle", getModuleTitle(module));

        // Load user
        PreparedStatement ps = db.prepareStatement("SELECT full_name, role FROM users WHERE id = ?");
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            ctx.put("userFullName", rs.getString("full_name"));
            ctx.put("userRole", rs.getString("role"));
        }
        rs.close(); ps.close();

        // Success message from redirect
        String msg = params.getOrDefault("msg", "");
        if ("success".equals(msg)) {
            ctx.put("flashMessage", "<div class=\"flash success\">Data berhasil disimpan!</div>");
        } else if ("deleted".equals(msg)) {
            ctx.put("flashMessage", "<div class=\"flash success\">Data berhasil dihapus!</div>");
        } else {
            ctx.put("flashMessage", "");
        }

        // Show form or data table
        if ("add".equals(action)) {
            ctx.put("moduleContent", buildModuleForm(module, null));
            ctx.put("moduleTitle", getModuleTitle(module) + " — Tambah Baru");
        } else if ("edit".equals(action) && params.containsKey("id")) {
            Map<String, String> data = loadRecord(module, Integer.parseInt(params.get("id")));
            ctx.put("moduleContent", buildModuleForm(module, data));
            ctx.put("moduleTitle", getModuleTitle(module) + " — Edit");
        } else {
            ctx.put("moduleContent", buildModuleContent(module));
        }

        sendPage(exchange, 200, "RF.jsp", ctx);
    }

    /**
     * /OA_HTML/runforms.jsp?resp_app=...&resp_key=...&secgrp_key=...&start_func=...
     * Renders the launcher page that auto-triggers JavawsLauncher popup.
     */
    private void handleRunForms(HttpExchange exchange) throws Exception {
        int userId = requireAuth(exchange);
        if (userId < 0) return;

        Map<String, String> params = HttpUtil.parseQuery(exchange.getRequestURI().getQuery());
        String respApp = params.getOrDefault("resp_app", "ANTABOGA");
        String respKey = params.getOrDefault("resp_key", "STANDARD");
        String secgrpKey = params.getOrDefault("secgrp_key", "STANDARD");
        String startFunc = params.getOrDefault("start_func", "REPORT");

        Map<String, String> ctx = TemplateEngine.newContext();
        ctx.put("respApp", esc(respApp));
        ctx.put("respKey", esc(respKey));
        ctx.put("secgrpKey", esc(secgrpKey));
        ctx.put("startFunc", esc(startFunc));
        ctx.put("funcTitle", getFuncTitle(startFunc));
        ctx.put("serverHost", exchange.getRequestHeaders().getFirst("Host"));

        sendPage(exchange, 200, "runforms.jsp", ctx);
    }

    private String getFuncTitle(String func) {
        if (func == null) return "Unknown";
        switch (func.toUpperCase()) {
            case "REPORT_SISWA": return "Laporan Data Siswa";
            case "REPORT_GURU": return "Laporan Data Guru";
            case "REPORT_KEUANGAN": return "Laporan Keuangan";
            case "REPORT": return "Desktop Report";
            case "ENTRY_NILAI": return "Entry Nilai";
            case "ENTRY_ABSENSI": return "Entry Absensi";
            default: return func;
        }
    }

    /**
     * /OA_HTML/OA.jsp?page=xxx&aksi=xxx — Dynamic module page
     */
    private void handleModulePage(HttpExchange exchange) throws Exception {
        int userId = requireAuth(exchange);
        if (userId < 0) return;

        Map<String, String> params = HttpUtil.parseQuery(exchange.getRequestURI().getQuery());
        String page = params.getOrDefault("page", "");
        String aksi = params.getOrDefault("aksi", "VIEW");

        Map<String, String> ctx = TemplateEngine.newContext();
        ctx.put("page", page);
        ctx.put("aksi", aksi);
        ctx.put("pageTitle", page.isEmpty() ? "Antaboga" : page);
        ctx.put("pageContent", "<p>Halaman " + InputValidator.sanitize(page) + " (" + InputValidator.sanitize(aksi) + ")</p>");

        sendPage(exchange, 200, "OA.jsp", ctx);
    }

    /**
     * /OA_HTML/AppsLogout.jsp — Logout
     */
    private void handleLogout(HttpExchange exchange) throws Exception {
        String sessionId = HttpUtil.getCookie(exchange, "ANTABOGA_SESSION");
        if (sessionId != null) {
            int userId = sessions.validateSession(sessionId);
            if (userId > 0) {
                logAction(userId, "JSP_LOGOUT", "user", userId, "Logout via AppsLogout.jsp");
            }
            sessions.destroySession(sessionId);
        }
        HttpUtil.setSessionCookie(exchange, "", 0);

        exchange.getResponseHeaders().set("Location", "/OA_HTML/AppsLocalLogin.jsp");
        exchange.sendResponseHeaders(302, -1);
    }

    // ============ HELPERS ============

    private int requireAuth(HttpExchange exchange) throws Exception {
        String sessionId = HttpUtil.getCookie(exchange, "ANTABOGA_SESSION");
        int userId = sessions.validateSession(sessionId);
        if (userId < 0) {
            exchange.getResponseHeaders().set("Location", "/OA_HTML/AppsLocalLogin.jsp");
            exchange.sendResponseHeaders(302, -1);
        }
        return userId;
    }

    private void sendPage(HttpExchange exchange, int status, String template, Map<String, String> ctx) throws IOException {
        String html;
        try {
            html = TemplateEngine.render(template, ctx);
        } catch (Exception e) {
            html = "<html><body><h1>Error</h1><p>" + e.getMessage() + "</p></body></html>";
        }

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate");
        exchange.getResponseHeaders().set("Server", "Antaboga/1.2.0");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private byte[] readBody(HttpExchange exchange) throws IOException {
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int len;
        while ((len = exchange.getRequestBody().read(chunk)) != -1) {
            buf.write(chunk, 0, len);
        }
        return buf.toByteArray();
    }

    private String getModuleTitle(String module) {
        switch (module) {
            case "students": return "Manajemen Siswa";
            case "teachers": return "Manajemen Guru";
            case "academic": return "Akademik";
            case "finance": return "Keuangan";
            case "attendance": return "Absensi";
            case "report": return "Desktop Report";
            default: return "Dashboard";
        }
    }

    private String mapFuncToModule(String funcId) {
        if (funcId == null) return "dashboard";
        switch (funcId.toLowerCase()) {
            case "students": case "siswa": return "students";
            case "teachers": case "guru": return "teachers";
            case "academic": case "akademik": return "academic";
            case "finance": case "keuangan": return "finance";
            case "attendance": case "absensi": return "attendance";
            case "report": case "laporan": return "report";
            default: return funcId.toLowerCase();
        }
    }

    private String buildModuleContent(String module) {
        try {
            StringBuilder sb = new StringBuilder();
            switch (module) {
                case "students":
                    sb.append("<div class=\"tableHeader\"><span>Daftar Siswa</span><a href=\"/OA_HTML/RF.jsp?module=students&action=add\" class=\"btnAdd\">+ Tambah Siswa</a></div>");
                    ResultSet rs = db.createStatement().executeQuery("SELECT id, nis, full_name, gender, phone, status FROM students ORDER BY full_name LIMIT 50");
                    sb.append("<table class=\"oaTable\"><thead><tr><th>NIS</th><th>Nama Lengkap</th><th>Gender</th><th>Telepon</th><th>Status</th><th>Aksi</th></tr></thead><tbody>");
                    boolean hasData = false;
                    while (rs.next()) {
                        hasData = true;
                        int rid = rs.getInt("id");
                        sb.append("<tr><td>").append(esc(rs.getString("nis")))
                          .append("</td><td>").append(esc(rs.getString("full_name")))
                          .append("</td><td>").append(esc(rs.getString("gender")))
                          .append("</td><td>").append(esc(rs.getString("phone")))
                          .append("</td><td><span class=\"badge\">").append(esc(rs.getString("status")))
                          .append("</span></td><td class=\"actions\">")
                          .append("<a href=\"/OA_HTML/RF.jsp?module=students&action=edit&id=").append(rid).append("\" class=\"btnEdit\">Edit</a> ")
                          .append("<a href=\"/OA_HTML/RF.jsp?module=students&action=delete&id=").append(rid).append("\" class=\"btnDel\" onclick=\"return confirm('Yakin hapus siswa ini?')\">Hapus</a>")
                          .append("</td></tr>");
                    }
                    if (!hasData) sb.append("<tr><td colspan=\"6\" class=\"emptyRow\">Belum ada data siswa</td></tr>");
                    sb.append("</tbody></table>");
                    rs.close();
                    break;
                case "teachers":
                    sb.append("<div class=\"tableHeader\"><span>Daftar Guru</span><a href=\"/OA_HTML/RF.jsp?module=teachers&action=add\" class=\"btnAdd\">+ Tambah Guru</a></div>");
                    rs = db.createStatement().executeQuery("SELECT id, nip, full_name, subject_specialization, phone, status FROM teachers ORDER BY full_name LIMIT 50");
                    sb.append("<table class=\"oaTable\"><thead><tr><th>NIP</th><th>Nama Lengkap</th><th>Spesialisasi</th><th>Telepon</th><th>Status</th><th>Aksi</th></tr></thead><tbody>");
                    hasData = false;
                    while (rs.next()) {
                        hasData = true;
                        int rid = rs.getInt("id");
                        sb.append("<tr><td>").append(esc(rs.getString("nip")))
                          .append("</td><td>").append(esc(rs.getString("full_name")))
                          .append("</td><td>").append(esc(rs.getString("subject_specialization")))
                          .append("</td><td>").append(esc(rs.getString("phone")))
                          .append("</td><td><span class=\"badge\">").append(esc(rs.getString("status")))
                          .append("</span></td><td class=\"actions\">")
                          .append("<a href=\"/OA_HTML/RF.jsp?module=teachers&action=edit&id=").append(rid).append("\" class=\"btnEdit\">Edit</a> ")
                          .append("<a href=\"/OA_HTML/RF.jsp?module=teachers&action=delete&id=").append(rid).append("\" class=\"btnDel\" onclick=\"return confirm('Yakin hapus guru ini?')\">Hapus</a>")
                          .append("</td></tr>");
                    }
                    if (!hasData) sb.append("<tr><td colspan=\"6\" class=\"emptyRow\">Belum ada data guru</td></tr>");
                    sb.append("</tbody></table>");
                    rs.close();
                    break;
                case "finance":
                    Statement s = db.createStatement();
                    ResultSet r1 = s.executeQuery("SELECT COALESCE(SUM(amount),0) FROM payments");
                    r1.next(); String totalPaid = formatRp(r1.getDouble(1)); r1.close();
                    r1 = s.executeQuery("SELECT COALESCE(SUM(amount),0) FROM invoices WHERE status='unpaid'");
                    r1.next(); String totalUnpaid = formatRp(r1.getDouble(1)); r1.close();
                    s.close();
                    sb.append("<div class=\"oaSummary\">");
                    sb.append("<div class=\"oaSumCard\"><div class=\"oaSumLabel\">Total Terbayar</div><div class=\"oaSumValue success\">").append(totalPaid).append("</div></div>");
                    sb.append("<div class=\"oaSumCard\"><div class=\"oaSumLabel\">Belum Terbayar</div><div class=\"oaSumValue warning\">").append(totalUnpaid).append("</div></div>");
                    sb.append("</div>");
                    break;
                case "report":
                    sb.append("<div class=\"tableHeader\"><span>Fungsi Desktop (Java Web Start)</span></div>");
                    sb.append("<table class=\"oaTable\"><thead><tr><th>Kode Fungsi</th><th>Nama</th><th>Responsibility</th><th>Aksi</th></tr></thead><tbody>");
                    String[][] funcs = {
                        {"REPORT_SISWA", "Laporan Data Siswa", "99999_SISWA_KITA"},
                        {"REPORT_GURU", "Laporan Data Guru", "99999_GURU_KITA"},
                        {"REPORT_KEUANGAN", "Laporan Keuangan", "99999_KEUANGAN"},
                        {"ENTRY_NILAI", "Entry Nilai Siswa", "99999_AKADEMIK"},
                        {"ENTRY_ABSENSI", "Entry Absensi", "99999_ABSENSI"}
                    };
                    for (String[] f : funcs) {
                        sb.append("<tr><td><code>").append(f[0]).append("</code></td>")
                          .append("<td>").append(f[1]).append("</td>")
                          .append("<td><code>").append(f[2]).append("</code></td>")
                          .append("<td class=\"actions\"><a href=\"/OA_HTML/runforms.jsp?resp_app=SQLAP")
                          .append("&resp_key=").append(f[2])
                          .append("&secgrp_key=STANDARD")
                          .append("&start_func=").append(f[0])
                          .append("\" class=\"btnEdit\" style=\"background:rgba(201,162,39,0.12);border-color:rgba(201,162,39,0.25);color:#c9a227;\">🖥️ Launch</a></td></tr>");
                    }
                    sb.append("</tbody></table>");
                    break;
                default:
                    sb.append("<div class=\"oaSummary\">");
                    sb.append("<div class=\"oaSumCard\"><div class=\"oaSumLabel\">Total Siswa</div><div class=\"oaSumValue\">").append(countTable("students")).append("</div></div>");
                    sb.append("<div class=\"oaSumCard\"><div class=\"oaSumLabel\">Total Guru</div><div class=\"oaSumValue\">").append(countTable("teachers")).append("</div></div>");
                    sb.append("<div class=\"oaSumCard\"><div class=\"oaSumLabel\">Total Kelas</div><div class=\"oaSumValue\">").append(countTable("classes")).append("</div></div>");
                    sb.append("<div class=\"oaSumCard\"><div class=\"oaSumLabel\">Invoice Belum Bayar</div><div class=\"oaSumValue warning\">").append(countWhere("invoices", "status='unpaid'")).append("</div></div>");
                    sb.append("</div>");
            }
            return sb.toString();
        } catch (SQLException e) {
            return "<p class=\"errorText\">Gagal memuat data: " + esc(e.getMessage()) + "</p>";
        }
    }

    private int countTable(String table) throws SQLException {
        Statement s = db.createStatement();
        ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table);
        rs.next(); int c = rs.getInt(1); rs.close(); s.close();
        return c;
    }

    private int countWhere(String table, String where) throws SQLException {
        Statement s = db.createStatement();
        ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + where);
        rs.next(); int c = rs.getInt(1); rs.close(); s.close();
        return c;
    }

    private String formatRp(double amount) {
        return "Rp " + String.format("%,.0f", amount);
    }

    private String esc(String s) {
        if (s == null) return "-";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private void logAction(int userId, String action, String entityType, int entityId, String details) {
        try {
            PreparedStatement ps = db.prepareStatement(
                "INSERT INTO audit_log (user_id, action, entity_type, entity_id, details) VALUES (?,?,?,?,?)");
            ps.setInt(1, userId); ps.setString(2, action); ps.setString(3, entityType);
            ps.setInt(4, entityId); ps.setString(5, details);
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) {
            System.err.println("[AUDIT] " + e.getMessage());
        }
    }

    // ============ CRUD: FORMS ============

    private String buildModuleForm(String module, Map<String, String> data) {
        boolean isEdit = (data != null && data.containsKey("id"));
        String id = isEdit ? data.get("id") : "";
        String actionUrl = "/OA_HTML/RF.jsp?module=" + module + (isEdit ? "&id=" + id : "");
        StringBuilder sb = new StringBuilder();
        sb.append("<form method=\"POST\" action=\"").append(actionUrl).append("\" class=\"oaForm\">");
        if (isEdit) sb.append("<input type=\"hidden\" name=\"id\" value=\"").append(esc(id)).append("\">");

        switch (module) {
            case "students":
                sb.append(formField("NIS *", "nis", gd(data, "nis"), true));
                sb.append(formField("Nama Lengkap *", "fullName", gd(data, "fullName"), true));
                sb.append("<div class=\"formRow\">");
                sb.append(formSelect("Gender", "gender", gd(data, "gender"), new String[]{"", "L", "P"}, new String[]{"-- Pilih --", "Laki-laki", "Perempuan"}));
                sb.append(formField("Tempat Lahir", "birthPlace", gd(data, "birthPlace"), false));
                sb.append("</div><div class=\"formRow\">");
                sb.append(formField("Tanggal Lahir", "birthDate", gd(data, "birthDate"), false, "date"));
                sb.append(formField("Telepon", "phone", gd(data, "phone"), false));
                sb.append("</div>");
                sb.append(formField("Email", "email", gd(data, "email"), false, "email"));
                sb.append(formTextarea("Alamat", "address", gd(data, "address")));
                sb.append("<div class=\"formRow\">");
                sb.append(formField("Nama Orang Tua", "parentName", gd(data, "parentName"), false));
                sb.append(formField("Telepon Orang Tua", "parentPhone", gd(data, "parentPhone"), false));
                sb.append("</div>");
                break;
            case "teachers":
                sb.append(formField("NIP *", "nip", gd(data, "nip"), true));
                sb.append(formField("Nama Lengkap *", "fullName", gd(data, "fullName"), true));
                sb.append("<div class=\"formRow\">");
                sb.append(formSelect("Gender", "gender", gd(data, "gender"), new String[]{"", "L", "P"}, new String[]{"-- Pilih --", "Laki-laki", "Perempuan"}));
                sb.append(formField("Spesialisasi", "subjectSpecialization", gd(data, "subjectSpecialization"), false));
                sb.append("</div><div class=\"formRow\">");
                sb.append(formField("Telepon", "phone", gd(data, "phone"), false));
                sb.append(formField("Email", "email", gd(data, "email"), false, "email"));
                sb.append("</div>");
                sb.append(formField("Kualifikasi", "qualification", gd(data, "qualification"), false));
                sb.append(formTextarea("Alamat", "address", gd(data, "address")));
                break;
            default:
                sb.append("<p>Form untuk modul ini belum tersedia.</p>");
        }

        sb.append("<div class=\"formActions\">");
        sb.append("<a href=\"/OA_HTML/RF.jsp?module=").append(module).append("\" class=\"btnCancel\">Batal</a>");
        sb.append("<button type=\"submit\" class=\"btnSubmit\">").append(isEdit ? "Perbarui" : "Simpan").append("</button>");
        sb.append("</div></form>");
        return sb.toString();
    }

    private String formField(String label, String name, String val, boolean req) {
        return formField(label, name, val, req, "text");
    }

    private String formField(String label, String name, String val, boolean req, String type) {
        return "<div class=\"formGroup\"><label>" + label + "</label>"
            + "<input type=\"" + type + "\" name=\"" + name + "\" value=\"" + esc(val) + "\""
            + (req ? " required" : "") + "></div>";
    }

    private String formSelect(String label, String name, String val, String[] values, String[] labels) {
        StringBuilder sb = new StringBuilder("<div class=\"formGroup\"><label>" + label + "</label><select name=\"" + name + "\">");
        for (int i = 0; i < values.length; i++) {
            sb.append("<option value=\"").append(values[i]).append("\"")
              .append(values[i].equals(val) ? " selected" : "").append(">").append(labels[i]).append("</option>");
        }
        sb.append("</select></div>");
        return sb.toString();
    }

    private String formTextarea(String label, String name, String val) {
        return "<div class=\"formGroup\"><label>" + label + "</label>"
            + "<textarea name=\"" + name + "\" rows=\"3\">" + esc(val) + "</textarea></div>";
    }

    private String gd(Map<String, String> data, String key) {
        if (data == null) return "";
        return data.getOrDefault(key, "");
    }

    // ============ CRUD: SAVE ============

    private void handleModuleSave(String module, Map<String, String> form, int userId) throws SQLException {
        String id = form.getOrDefault("id", "");
        boolean isUpdate = !id.isEmpty();

        switch (module) {
            case "students":
                if (isUpdate) {
                    PreparedStatement ps = db.prepareStatement("UPDATE students SET full_name=?, gender=?, birth_date=NULLIF(?,'')"
                        + ", birth_place=?, address=?, phone=?, email=?, parent_name=?, parent_phone=?, updated_at=CURRENT_TIMESTAMP WHERE id=?");
                    ps.setString(1, form.getOrDefault("fullName", "")); ps.setString(2, form.getOrDefault("gender", ""));
                    ps.setString(3, form.getOrDefault("birthDate", "")); ps.setString(4, form.getOrDefault("birthPlace", ""));
                    ps.setString(5, form.getOrDefault("address", "")); ps.setString(6, form.getOrDefault("phone", ""));
                    ps.setString(7, form.getOrDefault("email", "")); ps.setString(8, form.getOrDefault("parentName", ""));
                    ps.setString(9, form.getOrDefault("parentPhone", "")); ps.setInt(10, Integer.parseInt(id));
                    ps.executeUpdate(); ps.close();
                    logAction(userId, "JSP_UPDATE_STUDENT", "student", Integer.parseInt(id), "Edit siswa via JSP");
                } else {
                    PreparedStatement ps = db.prepareStatement("INSERT INTO students (nis,full_name,gender,birth_date,birth_place,address,phone,email,parent_name,parent_phone) VALUES (?,?,?,NULLIF(?,''),?,?,?,?,?,?)");
                    ps.setString(1, form.getOrDefault("nis", "")); ps.setString(2, form.getOrDefault("fullName", ""));
                    ps.setString(3, form.getOrDefault("gender", "")); ps.setString(4, form.getOrDefault("birthDate", ""));
                    ps.setString(5, form.getOrDefault("birthPlace", "")); ps.setString(6, form.getOrDefault("address", ""));
                    ps.setString(7, form.getOrDefault("phone", "")); ps.setString(8, form.getOrDefault("email", ""));
                    ps.setString(9, form.getOrDefault("parentName", "")); ps.setString(10, form.getOrDefault("parentPhone", ""));
                    ps.executeUpdate(); ps.close();
                    logAction(userId, "JSP_CREATE_STUDENT", "student", 0, "Tambah siswa via JSP");
                }
                break;
            case "teachers":
                if (isUpdate) {
                    PreparedStatement ps = db.prepareStatement("UPDATE teachers SET full_name=?, gender=?, address=?, phone=?, email=?"
                        + ", subject_specialization=?, qualification=?, updated_at=CURRENT_TIMESTAMP WHERE id=?");
                    ps.setString(1, form.getOrDefault("fullName", "")); ps.setString(2, form.getOrDefault("gender", ""));
                    ps.setString(3, form.getOrDefault("address", "")); ps.setString(4, form.getOrDefault("phone", ""));
                    ps.setString(5, form.getOrDefault("email", "")); ps.setString(6, form.getOrDefault("subjectSpecialization", ""));
                    ps.setString(7, form.getOrDefault("qualification", "")); ps.setInt(8, Integer.parseInt(id));
                    ps.executeUpdate(); ps.close();
                    logAction(userId, "JSP_UPDATE_TEACHER", "teacher", Integer.parseInt(id), "Edit guru via JSP");
                } else {
                    PreparedStatement ps = db.prepareStatement("INSERT INTO teachers (nip,full_name,gender,address,phone,email,subject_specialization,qualification) VALUES (?,?,?,?,?,?,?,?)");
                    ps.setString(1, form.getOrDefault("nip", "")); ps.setString(2, form.getOrDefault("fullName", ""));
                    ps.setString(3, form.getOrDefault("gender", "")); ps.setString(4, form.getOrDefault("address", ""));
                    ps.setString(5, form.getOrDefault("phone", "")); ps.setString(6, form.getOrDefault("email", ""));
                    ps.setString(7, form.getOrDefault("subjectSpecialization", "")); ps.setString(8, form.getOrDefault("qualification", ""));
                    ps.executeUpdate(); ps.close();
                    logAction(userId, "JSP_CREATE_TEACHER", "teacher", 0, "Tambah guru via JSP");
                }
                break;
        }
    }

    // ============ CRUD: DELETE ============

    private void handleModuleDelete(String module, int id, int userId) throws SQLException {
        String table;
        switch (module) {
            case "students": table = "students"; break;
            case "teachers": table = "teachers"; break;
            default: return;
        }
        PreparedStatement ps = db.prepareStatement("DELETE FROM " + table + " WHERE id = ?");
        ps.setInt(1, id);
        ps.executeUpdate(); ps.close();
        logAction(userId, "JSP_DELETE", table, id, "Hapus dari " + table + " via JSP");
    }

    // ============ CRUD: LOAD RECORD FOR EDIT ============

    private Map<String, String> loadRecord(String module, int id) throws SQLException {
        Map<String, String> data = new java.util.HashMap<>();
        String sql;
        switch (module) {
            case "students": sql = "SELECT * FROM students WHERE id = ?"; break;
            case "teachers": sql = "SELECT * FROM teachers WHERE id = ?"; break;
            default: return data;
        }
        PreparedStatement ps = db.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            java.sql.ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String col = meta.getColumnName(i);
                String val = rs.getString(i);
                // Convert snake_case to camelCase
                String camel = toCamel(col);
                data.put(camel, val != null ? val : "");
            }
            data.put("id", String.valueOf(rs.getInt("id")));
        }
        rs.close(); ps.close();
        return data;
    }

    private String toCamel(String snake) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : snake.toLowerCase().toCharArray()) {
            if (c == '_') { upper = true; }
            else { sb.append(upper ? Character.toUpperCase(c) : c); upper = false; }
        }
        return sb.toString();
    }
}
