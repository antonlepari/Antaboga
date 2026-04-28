package id.kamsib.antaboga.modules.teacher;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import id.kamsib.antaboga.security.InputValidator;
import id.kamsib.antaboga.security.SessionManager;
import id.kamsib.antaboga.server.HttpUtil;

import java.io.IOException;
import java.sql.*;
import java.util.Map;

public class TeacherHandler implements HttpHandler {
    private final Connection db;
    private final SessionManager sessions;

    public TeacherHandler(Connection db, SessionManager sessions) {
        this.db = db;
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String sessionId = HttpUtil.getCookie(exchange, "ANTABOGA_SESSION");
        try {
            int userId = sessions.validateSession(sessionId);
            if (userId < 0) { HttpUtil.sendError(exchange, 401, "Sesi tidak valid"); return; }

            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method)) {
                if (path.matches(".*/teachers/\\d+$")) {
                    int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                    getTeacher(exchange, id);
                } else {
                    listTeachers(exchange);
                }
            } else if ("POST".equals(method)) {
                createTeacher(exchange, userId);
            } else if ("PUT".equals(method) && path.matches(".*/teachers/\\d+$")) {
                int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                updateTeacher(exchange, id, userId);
            } else if ("DELETE".equals(method) && path.matches(".*/teachers/\\d+$")) {
                int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                deleteTeacher(exchange, id, userId);
            } else {
                HttpUtil.sendError(exchange, 405, "Method tidak diizinkan");
            }
        } catch (Exception e) {
            System.err.println("[TEACHER ERROR] " + e.getMessage());
            HttpUtil.sendError(exchange, 500, "Terjadi kesalahan server");
        }
    }

    private void listTeachers(HttpExchange exchange) throws Exception {
        Map<String, String> params = HttpUtil.parseQuery(exchange.getRequestURI().getQuery());
        int page = InputValidator.parseIntSafe(params.get("page"), 1);
        int limit = InputValidator.parseIntSafe(params.get("limit"), 20);
        String search = InputValidator.clean(params.getOrDefault("search", ""));

        String where = search.isEmpty() ? "" : " WHERE full_name LIKE ? OR nip LIKE ?";
        PreparedStatement countPs = db.prepareStatement("SELECT COUNT(*) FROM teachers" + where);
        if (!search.isEmpty()) { countPs.setString(1, "%" + search + "%"); countPs.setString(2, "%" + search + "%"); }
        ResultSet crs = countPs.executeQuery(); crs.next();
        int total = crs.getInt(1); crs.close(); countPs.close();

        PreparedStatement ps = db.prepareStatement("SELECT * FROM teachers" + where + " ORDER BY full_name LIMIT ? OFFSET ?");
        int idx = 1;
        if (!search.isEmpty()) { ps.setString(idx++, "%" + search + "%"); ps.setString(idx++, "%" + search + "%"); }
        ps.setInt(idx++, limit); ps.setInt(idx, (page - 1) * limit);
        ResultSet rs = ps.executeQuery();

        JsonArray arr = new JsonArray();
        while (rs.next()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", rs.getInt("id"));
            obj.addProperty("nip", rs.getString("nip"));
            obj.addProperty("fullName", rs.getString("full_name"));
            obj.addProperty("gender", rs.getString("gender"));
            obj.addProperty("phone", rs.getString("phone"));
            obj.addProperty("email", rs.getString("email"));
            obj.addProperty("subjectSpecialization", rs.getString("subject_specialization"));
            obj.addProperty("qualification", rs.getString("qualification"));
            obj.addProperty("status", rs.getString("status"));
            arr.add(obj);
        }
        rs.close(); ps.close();

        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("total", total);
        resp.addProperty("page", page);
        resp.add("data", arr);
        HttpUtil.sendJson(exchange, 200, resp);
    }

    private void getTeacher(HttpExchange exchange, int id) throws Exception {
        PreparedStatement ps = db.prepareStatement("SELECT * FROM teachers WHERE id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", rs.getInt("id"));
            obj.addProperty("nip", rs.getString("nip"));
            obj.addProperty("fullName", rs.getString("full_name"));
            obj.addProperty("gender", rs.getString("gender"));
            obj.addProperty("birthDate", rs.getString("birth_date"));
            obj.addProperty("address", rs.getString("address"));
            obj.addProperty("phone", rs.getString("phone"));
            obj.addProperty("email", rs.getString("email"));
            obj.addProperty("subjectSpecialization", rs.getString("subject_specialization"));
            obj.addProperty("qualification", rs.getString("qualification"));
            obj.addProperty("status", rs.getString("status"));
            JsonObject resp = new JsonObject();
            resp.addProperty("success", true);
            resp.add("data", obj);
            HttpUtil.sendJson(exchange, 200, resp);
        } else {
            HttpUtil.sendError(exchange, 404, "Guru tidak ditemukan");
        }
        rs.close(); ps.close();
    }

    private void createTeacher(HttpExchange exchange, int userId) throws Exception {
        JsonObject body = HttpUtil.parseJsonBody(exchange);
        String nip = InputValidator.clean(body.has("nip") ? body.get("nip").getAsString() : "");
        String name = InputValidator.clean(body.has("fullName") ? body.get("fullName").getAsString() : "");

        if (!InputValidator.isValidNisNip(nip)) { HttpUtil.sendError(exchange, 400, "NIP tidak valid"); return; }
        if (!InputValidator.isValidLength(name, 2, 100)) { HttpUtil.sendError(exchange, 400, "Nama harus 2-100 karakter"); return; }

        PreparedStatement ps = db.prepareStatement(
            "INSERT INTO teachers (nip, full_name, gender, birth_date, address, phone, email, subject_specialization, qualification) VALUES (?,?,?,?,?,?,?,?,?)");
        ps.setString(1, nip); ps.setString(2, name);
        ps.setString(3, getStr(body, "gender")); ps.setString(4, getStr(body, "birthDate"));
        ps.setString(5, getStr(body, "address")); ps.setString(6, getStr(body, "phone"));
        ps.setString(7, getStr(body, "email")); ps.setString(8, getStr(body, "subjectSpecialization"));
        ps.setString(9, getStr(body, "qualification"));
        ps.executeUpdate(); ps.close();
        HttpUtil.sendSuccess(exchange, "Guru berhasil ditambahkan", null);
    }

    private void updateTeacher(HttpExchange exchange, int id, int userId) throws Exception {
        JsonObject body = HttpUtil.parseJsonBody(exchange);
        PreparedStatement ps = db.prepareStatement(
            "UPDATE teachers SET full_name=?, gender=?, birth_date=?, address=?, phone=?, email=?, "
            + "subject_specialization=?, qualification=?, updated_at=CURRENT_TIMESTAMP WHERE id=?");
        ps.setString(1, InputValidator.clean(getStr(body, "fullName")));
        ps.setString(2, getStr(body, "gender")); ps.setString(3, getStr(body, "birthDate"));
        ps.setString(4, getStr(body, "address")); ps.setString(5, getStr(body, "phone"));
        ps.setString(6, getStr(body, "email")); ps.setString(7, getStr(body, "subjectSpecialization"));
        ps.setString(8, getStr(body, "qualification")); ps.setInt(9, id);
        int rows = ps.executeUpdate(); ps.close();
        if (rows > 0) HttpUtil.sendSuccess(exchange, "Guru berhasil diperbarui", null);
        else HttpUtil.sendError(exchange, 404, "Guru tidak ditemukan");
    }

    private void deleteTeacher(HttpExchange exchange, int id, int userId) throws Exception {
        PreparedStatement ps = db.prepareStatement("DELETE FROM teachers WHERE id = ?");
        ps.setInt(1, id);
        int rows = ps.executeUpdate(); ps.close();
        if (rows > 0) HttpUtil.sendSuccess(exchange, "Guru berhasil dihapus", null);
        else HttpUtil.sendError(exchange, 404, "Guru tidak ditemukan");
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }
}
