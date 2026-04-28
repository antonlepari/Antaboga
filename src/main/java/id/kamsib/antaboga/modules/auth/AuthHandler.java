package id.kamsib.antaboga.modules.auth;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import id.kamsib.antaboga.security.InputValidator;
import id.kamsib.antaboga.security.PasswordHasher;
import id.kamsib.antaboga.security.SessionManager;
import id.kamsib.antaboga.server.HttpUtil;

import java.io.IOException;
import java.sql.*;

public class AuthHandler implements HttpHandler {
    private final Connection db;
    private final SessionManager sessions;

    public AuthHandler(Connection db, SessionManager sessions) {
        this.db = db;
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String ip = HttpUtil.getClientIp(exchange);
        if (!sessions.checkRateLimit(ip)) {
            HttpUtil.sendError(exchange, 429, "Terlalu banyak permintaan. Coba lagi nanti.");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.endsWith("/login") && "POST".equals(method)) {
                handleLogin(exchange);
            } else if (path.endsWith("/logout") && "POST".equals(method)) {
                handleLogout(exchange);
            } else if (path.endsWith("/me") && "GET".equals(method)) {
                handleMe(exchange);
            } else if (path.endsWith("/change-password") && "POST".equals(method)) {
                handleChangePassword(exchange);
            } else {
                HttpUtil.sendError(exchange, 404, "Endpoint tidak ditemukan");
            }
        } catch (Exception e) {
            System.err.println("[AUTH ERROR] " + e.getMessage());
            HttpUtil.sendError(exchange, 500, "Terjadi kesalahan server");
        }
    }

    private void handleLogin(HttpExchange exchange) throws Exception {
        JsonObject body = HttpUtil.parseJsonBody(exchange);
        String username = InputValidator.clean(body.has("username") ? body.get("username").getAsString() : "");
        String password = body.has("password") ? body.get("password").getAsString() : "";

        if (!InputValidator.isNotEmpty(username) || !InputValidator.isNotEmpty(password)) {
            HttpUtil.sendError(exchange, 400, "Username dan password harus diisi");
            return;
        }

        PreparedStatement ps = db.prepareStatement(
            "SELECT id, password_hash, full_name, role, is_active FROM users WHERE username = ?");
        ps.setString(1, username);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            if (!rs.getBoolean("is_active")) {
                HttpUtil.sendError(exchange, 403, "Akun tidak aktif");
                rs.close(); ps.close();
                return;
            }

            if (PasswordHasher.verify(password, rs.getString("password_hash"))) {
                int userId = rs.getInt("id");
                String sessionId = sessions.createSession(userId);
                String csrf = sessions.getCsrfToken(sessionId);

                // Audit log
                logAction(userId, "LOGIN", "user", userId, "Login berhasil dari " + HttpUtil.getClientIp(exchange));

                HttpUtil.setSessionCookie(exchange, sessionId, 3600);

                JsonObject resp = new JsonObject();
                resp.addProperty("success", true);
                resp.addProperty("message", "Login berhasil");
                resp.addProperty("fullName", rs.getString("full_name"));
                resp.addProperty("role", rs.getString("role"));
                resp.addProperty("csrfToken", csrf);
                resp.addProperty("sessionId", sessionId);
                HttpUtil.sendJson(exchange, 200, resp);
                rs.close(); ps.close();
                return;
            }
        }

        rs.close(); ps.close();
        HttpUtil.sendError(exchange, 401, "Username atau password salah");
    }

    private void handleLogout(HttpExchange exchange) throws Exception {
        String sessionId = HttpUtil.getCookie(exchange, "ANTABOGA_SESSION");
        if (sessionId != null) {
            sessions.destroySession(sessionId);
        }
        HttpUtil.setSessionCookie(exchange, "", 0);

        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("message", "Logout berhasil");
        HttpUtil.sendJson(exchange, 200, resp);
    }

    private void handleMe(HttpExchange exchange) throws Exception {
        String sessionId = HttpUtil.getCookie(exchange, "ANTABOGA_SESSION");
        int userId = sessions.validateSession(sessionId);

        if (userId < 0) {
            HttpUtil.sendError(exchange, 401, "Sesi tidak valid");
            return;
        }

        PreparedStatement ps = db.prepareStatement(
            "SELECT id, username, full_name, email, role FROM users WHERE id = ?");
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            JsonObject user = new JsonObject();
            user.addProperty("id", rs.getInt("id"));
            user.addProperty("username", rs.getString("username"));
            user.addProperty("fullName", rs.getString("full_name"));
            user.addProperty("email", rs.getString("email"));
            user.addProperty("role", rs.getString("role"));
            user.addProperty("csrfToken", sessions.getCsrfToken(sessionId));

            JsonObject resp = new JsonObject();
            resp.addProperty("success", true);
            resp.add("user", user);
            HttpUtil.sendJson(exchange, 200, resp);
        } else {
            HttpUtil.sendError(exchange, 404, "User tidak ditemukan");
        }

        rs.close(); ps.close();
    }

    private void handleChangePassword(HttpExchange exchange) throws Exception {
        String sessionId = HttpUtil.getCookie(exchange, "ANTABOGA_SESSION");
        int userId = sessions.validateSession(sessionId);
        if (userId < 0) {
            HttpUtil.sendError(exchange, 401, "Sesi tidak valid");
            return;
        }

        JsonObject body = HttpUtil.parseJsonBody(exchange);
        String oldPass = body.has("oldPassword") ? body.get("oldPassword").getAsString() : "";
        String newPass = body.has("newPassword") ? body.get("newPassword").getAsString() : "";

        if (newPass.length() < 8) {
            HttpUtil.sendError(exchange, 400, "Password baru minimal 8 karakter");
            return;
        }

        PreparedStatement ps = db.prepareStatement("SELECT password_hash FROM users WHERE id = ?");
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        if (rs.next() && PasswordHasher.verify(oldPass, rs.getString("password_hash"))) {
            PreparedStatement upd = db.prepareStatement(
                "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?");
            upd.setString(1, PasswordHasher.hash(newPass));
            upd.setInt(2, userId);
            upd.executeUpdate();
            upd.close();

            logAction(userId, "CHANGE_PASSWORD", "user", userId, "Password diubah");
            HttpUtil.sendSuccess(exchange, "Password berhasil diubah", null);
        } else {
            HttpUtil.sendError(exchange, 400, "Password lama salah");
        }
        rs.close(); ps.close();
    }

    private void logAction(int userId, String action, String entityType, int entityId, String details) {
        try {
            PreparedStatement ps = db.prepareStatement(
                "INSERT INTO audit_log (user_id, action, entity_type, entity_id, details) VALUES (?,?,?,?,?)");
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, entityType);
            ps.setInt(4, entityId);
            ps.setString(5, details);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.err.println("[AUDIT] Failed to log: " + e.getMessage());
        }
    }
}
