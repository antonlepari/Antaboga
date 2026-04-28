package id.kamsib.antaboga.modules.student;

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

public class StudentHandler implements HttpHandler {
    private final Connection db;
    private final SessionManager sessions;

    public StudentHandler(Connection db, SessionManager sessions) {
        this.db = db;
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String sessionId = HttpUtil.getCookie(exchange, "ANTABOGA_SESSION");
        try {
            int userId = sessions.validateSession(sessionId);
            if (userId < 0) {
                HttpUtil.sendError(exchange, 401, "Sesi tidak valid");
                return;
            }

            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            switch (method) {
                case "GET":
                    if (path.matches(".*/students/\\d+$")) {
                        String idStr = path.substring(path.lastIndexOf('/') + 1);
                        getStudent(exchange, Integer.parseInt(idStr));
                    } else {
                        listStudents(exchange);
                    }
                    break;
                case "POST":
                    createStudent(exchange, userId);
                    break;
                case "PUT":
                    if (path.matches(".*/students/\\d+$")) {
                        String idStr = path.substring(path.lastIndexOf('/') + 1);
                        updateStudent(exchange, Integer.parseInt(idStr), userId);
                    } else {
                        HttpUtil.sendError(exchange, 400, "ID siswa diperlukan");
                    }
                    break;
                case "DELETE":
                    if (path.matches(".*/students/\\d+$")) {
                        String idStr = path.substring(path.lastIndexOf('/') + 1);
                        deleteStudent(exchange, Integer.parseInt(idStr), userId);
                    } else {
                        HttpUtil.sendError(exchange, 400, "ID siswa diperlukan");
                    }
                    break;
                default:
                    HttpUtil.sendError(exchange, 405, "Method tidak diizinkan");
            }
        } catch (Exception e) {
            System.err.println("[STUDENT ERROR] " + e.getMessage());
            HttpUtil.sendError(exchange, 500, "Terjadi kesalahan server");
        }
    }

    private void listStudents(HttpExchange exchange) throws Exception {
        Map<String, String> params = HttpUtil.parseQuery(exchange.getRequestURI().getQuery());
        int page = InputValidator.parseIntSafe(params.get("page"), 1);
        int limit = InputValidator.parseIntSafe(params.get("limit"), 20);
        String search = InputValidator.clean(params.getOrDefault("search", ""));
        int offset = (page - 1) * limit;

        String baseQuery = "FROM students";
        String where = "";
        if (!search.isEmpty()) {
            where = " WHERE full_name LIKE ? OR nis LIKE ?";
        }

        // Count
        PreparedStatement countPs = db.prepareStatement("SELECT COUNT(*) " + baseQuery + where);
        if (!search.isEmpty()) {
            countPs.setString(1, "%" + search + "%");
            countPs.setString(2, "%" + search + "%");
        }
        ResultSet countRs = countPs.executeQuery();
        countRs.next();
        int total = countRs.getInt(1);
        countRs.close(); countPs.close();

        // Data
        PreparedStatement ps = db.prepareStatement(
            "SELECT * " + baseQuery + where + " ORDER BY full_name LIMIT ? OFFSET ?");
        int idx = 1;
        if (!search.isEmpty()) {
            ps.setString(idx++, "%" + search + "%");
            ps.setString(idx++, "%" + search + "%");
        }
        ps.setInt(idx++, limit);
        ps.setInt(idx, offset);
        ResultSet rs = ps.executeQuery();

        JsonArray arr = new JsonArray();
        while (rs.next()) {
            arr.add(studentToJson(rs));
        }
        rs.close(); ps.close();

        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("total", total);
        resp.addProperty("page", page);
        resp.addProperty("totalPages", (int) Math.ceil((double) total / limit));
        resp.add("data", arr);
        HttpUtil.sendJson(exchange, 200, resp);
    }

    private void getStudent(HttpExchange exchange, int id) throws Exception {
        PreparedStatement ps = db.prepareStatement("SELECT * FROM students WHERE id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            JsonObject resp = new JsonObject();
            resp.addProperty("success", true);
            resp.add("data", studentToJson(rs));
            HttpUtil.sendJson(exchange, 200, resp);
        } else {
            HttpUtil.sendError(exchange, 404, "Siswa tidak ditemukan");
        }
        rs.close(); ps.close();
    }

    private void createStudent(HttpExchange exchange, int userId) throws Exception {
        JsonObject body = HttpUtil.parseJsonBody(exchange);
        String nis = InputValidator.clean(body.has("nis") ? body.get("nis").getAsString() : "");
        String name = InputValidator.clean(body.has("fullName") ? body.get("fullName").getAsString() : "");

        if (!InputValidator.isValidNisNip(nis)) {
            HttpUtil.sendError(exchange, 400, "NIS tidak valid");
            return;
        }
        if (!InputValidator.isValidLength(name, 2, 100)) {
            HttpUtil.sendError(exchange, 400, "Nama harus 2-100 karakter");
            return;
        }

        PreparedStatement ps = db.prepareStatement(
            "INSERT INTO students (nis, full_name, gender, birth_date, birth_place, address, phone, email, parent_name, parent_phone) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, nis);
        ps.setString(2, name);
        ps.setString(3, getStr(body, "gender"));
        ps.setString(4, getStr(body, "birthDate"));
        ps.setString(5, getStr(body, "birthPlace"));
        ps.setString(6, getStr(body, "address"));
        ps.setString(7, getStr(body, "phone"));
        ps.setString(8, getStr(body, "email"));
        ps.setString(9, getStr(body, "parentName"));
        ps.setString(10, getStr(body, "parentPhone"));
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        int newId = keys.next() ? keys.getInt(1) : 0;
        keys.close(); ps.close();

        logAction(userId, "CREATE_STUDENT", "student", newId, "Siswa baru: " + name);
        HttpUtil.sendSuccess(exchange, "Siswa berhasil ditambahkan", null);
    }

    private void updateStudent(HttpExchange exchange, int id, int userId) throws Exception {
        JsonObject body = HttpUtil.parseJsonBody(exchange);
        PreparedStatement ps = db.prepareStatement(
            "UPDATE students SET full_name=?, gender=?, birth_date=?, birth_place=?, "
            + "address=?, phone=?, email=?, parent_name=?, parent_phone=?, updated_at=CURRENT_TIMESTAMP WHERE id=?");
        ps.setString(1, InputValidator.clean(getStr(body, "fullName")));
        ps.setString(2, getStr(body, "gender"));
        ps.setString(3, getStr(body, "birthDate"));
        ps.setString(4, getStr(body, "birthPlace"));
        ps.setString(5, getStr(body, "address"));
        ps.setString(6, getStr(body, "phone"));
        ps.setString(7, getStr(body, "email"));
        ps.setString(8, getStr(body, "parentName"));
        ps.setString(9, getStr(body, "parentPhone"));
        ps.setInt(10, id);
        int rows = ps.executeUpdate();
        ps.close();

        if (rows > 0) {
            logAction(userId, "UPDATE_STUDENT", "student", id, "Siswa diperbarui");
            HttpUtil.sendSuccess(exchange, "Siswa berhasil diperbarui", null);
        } else {
            HttpUtil.sendError(exchange, 404, "Siswa tidak ditemukan");
        }
    }

    private void deleteStudent(HttpExchange exchange, int id, int userId) throws Exception {
        PreparedStatement ps = db.prepareStatement("DELETE FROM students WHERE id = ?");
        ps.setInt(1, id);
        int rows = ps.executeUpdate();
        ps.close();

        if (rows > 0) {
            logAction(userId, "DELETE_STUDENT", "student", id, "Siswa dihapus");
            HttpUtil.sendSuccess(exchange, "Siswa berhasil dihapus", null);
        } else {
            HttpUtil.sendError(exchange, 404, "Siswa tidak ditemukan");
        }
    }

    private JsonObject studentToJson(ResultSet rs) throws SQLException {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", rs.getInt("id"));
        obj.addProperty("nis", rs.getString("nis"));
        obj.addProperty("fullName", rs.getString("full_name"));
        obj.addProperty("gender", rs.getString("gender"));
        obj.addProperty("birthDate", rs.getString("birth_date"));
        obj.addProperty("birthPlace", rs.getString("birth_place"));
        obj.addProperty("address", rs.getString("address"));
        obj.addProperty("phone", rs.getString("phone"));
        obj.addProperty("email", rs.getString("email"));
        obj.addProperty("parentName", rs.getString("parent_name"));
        obj.addProperty("parentPhone", rs.getString("parent_phone"));
        obj.addProperty("status", rs.getString("status"));
        return obj;
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
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
            System.err.println("[AUDIT] " + e.getMessage());
        }
    }
}
