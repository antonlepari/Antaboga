package id.kamsib.antaboga.modules.academic;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import id.kamsib.antaboga.security.InputValidator;
import id.kamsib.antaboga.security.SessionManager;
import id.kamsib.antaboga.server.HttpUtil;

import java.io.IOException;
import java.sql.*;

public class AcademicHandler implements HttpHandler {
    private final Connection db;
    private final SessionManager sessions;

    public AcademicHandler(Connection db, SessionManager sessions) {
        this.db = db;
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String sessionId = HttpUtil.getCookie(exchange, "ANTABOGA_SESSION");
        try {
            int userId = sessions.validateSession(sessionId);
            if (userId < 0) { HttpUtil.sendError(exchange, 401, "Sesi tidak valid"); return; }
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (path.contains("/classes")) handleClasses(exchange, method);
            else if (path.contains("/subjects")) handleSubjects(exchange, method);
            else if (path.contains("/grades")) handleGrades(exchange, method, userId);
            else if (path.contains("/years")) handleYears(exchange, method);
            else HttpUtil.sendError(exchange, 404, "Endpoint tidak ditemukan");
        } catch (Exception e) {
            HttpUtil.sendError(exchange, 500, "Terjadi kesalahan server");
        }
    }

    private void handleClasses(HttpExchange ex, String method) throws Exception {
        if ("GET".equals(method)) {
            ResultSet rs = db.createStatement().executeQuery(
                "SELECT c.*, t.full_name as teacher_name FROM classes c LEFT JOIN teachers t ON c.homeroom_teacher_id = t.id ORDER BY c.name");
            JsonArray arr = new JsonArray();
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", rs.getInt("id")); o.addProperty("name", rs.getString("name"));
                o.addProperty("gradeLevel", rs.getString("grade_level")); o.addProperty("capacity", rs.getInt("capacity"));
                o.addProperty("teacherName", rs.getString("teacher_name")); arr.add(o);
            }
            rs.close();
            JsonObject resp = new JsonObject(); resp.addProperty("success", true); resp.add("data", arr);
            HttpUtil.sendJson(ex, 200, resp);
        } else if ("POST".equals(method)) {
            JsonObject body = HttpUtil.parseJsonBody(ex);
            PreparedStatement ps = db.prepareStatement("INSERT INTO classes (name, grade_level, capacity) VALUES (?,?,?)");
            ps.setString(1, InputValidator.clean(gs(body, "name"))); ps.setString(2, gs(body, "gradeLevel"));
            ps.setInt(3, body.has("capacity") ? body.get("capacity").getAsInt() : 30);
            ps.executeUpdate(); ps.close();
            HttpUtil.sendSuccess(ex, "Kelas berhasil ditambahkan", null);
        }
    }

    private void handleSubjects(HttpExchange ex, String method) throws Exception {
        if ("GET".equals(method)) {
            ResultSet rs = db.createStatement().executeQuery("SELECT * FROM subjects ORDER BY name");
            JsonArray arr = new JsonArray();
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", rs.getInt("id")); o.addProperty("code", rs.getString("code"));
                o.addProperty("name", rs.getString("name")); o.addProperty("credits", rs.getInt("credits")); arr.add(o);
            }
            rs.close();
            JsonObject resp = new JsonObject(); resp.addProperty("success", true); resp.add("data", arr);
            HttpUtil.sendJson(ex, 200, resp);
        } else if ("POST".equals(method)) {
            JsonObject body = HttpUtil.parseJsonBody(ex);
            PreparedStatement ps = db.prepareStatement("INSERT INTO subjects (code, name, description, credits) VALUES (?,?,?,?)");
            ps.setString(1, InputValidator.clean(gs(body, "code"))); ps.setString(2, InputValidator.clean(gs(body, "name")));
            ps.setString(3, gs(body, "description")); ps.setInt(4, body.has("credits") ? body.get("credits").getAsInt() : 0);
            ps.executeUpdate(); ps.close();
            HttpUtil.sendSuccess(ex, "Mata pelajaran berhasil ditambahkan", null);
        }
    }

    private void handleGrades(HttpExchange ex, String method, int userId) throws Exception {
        if ("GET".equals(method)) {
            String studentId = HttpUtil.parseQuery(ex.getRequestURI().getQuery()).get("studentId");
            if (studentId == null) { HttpUtil.sendError(ex, 400, "studentId diperlukan"); return; }
            PreparedStatement ps = db.prepareStatement(
                "SELECT g.*, s.name as subject_name FROM grades g JOIN subjects s ON g.subject_id = s.id WHERE g.student_id = ?");
            ps.setInt(1, Integer.parseInt(studentId));
            ResultSet rs = ps.executeQuery();
            JsonArray arr = new JsonArray();
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", rs.getInt("id")); o.addProperty("subjectName", rs.getString("subject_name"));
                o.addProperty("gradeType", rs.getString("grade_type")); o.addProperty("score", rs.getDouble("score")); arr.add(o);
            }
            rs.close(); ps.close();
            JsonObject resp = new JsonObject(); resp.addProperty("success", true); resp.add("data", arr);
            HttpUtil.sendJson(ex, 200, resp);
        } else if ("POST".equals(method)) {
            JsonObject body = HttpUtil.parseJsonBody(ex);
            PreparedStatement ps = db.prepareStatement(
                "INSERT INTO grades (student_id, subject_id, grade_type, score, remarks, recorded_by) VALUES (?,?,?,?,?,?)");
            ps.setInt(1, body.get("studentId").getAsInt()); ps.setInt(2, body.get("subjectId").getAsInt());
            ps.setString(3, gs(body, "gradeType")); ps.setDouble(4, body.get("score").getAsDouble());
            ps.setString(5, gs(body, "remarks")); ps.setInt(6, userId);
            ps.executeUpdate(); ps.close();
            HttpUtil.sendSuccess(ex, "Nilai berhasil ditambahkan", null);
        }
    }

    private void handleYears(HttpExchange ex, String method) throws Exception {
        if ("GET".equals(method)) {
            ResultSet rs = db.createStatement().executeQuery("SELECT * FROM academic_years ORDER BY start_date DESC");
            JsonArray arr = new JsonArray();
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", rs.getInt("id")); o.addProperty("name", rs.getString("name"));
                o.addProperty("startDate", rs.getString("start_date")); o.addProperty("endDate", rs.getString("end_date"));
                o.addProperty("isActive", rs.getBoolean("is_active")); arr.add(o);
            }
            rs.close();
            JsonObject resp = new JsonObject(); resp.addProperty("success", true); resp.add("data", arr);
            HttpUtil.sendJson(ex, 200, resp);
        } else if ("POST".equals(method)) {
            JsonObject body = HttpUtil.parseJsonBody(ex);
            PreparedStatement ps = db.prepareStatement("INSERT INTO academic_years (name, start_date, end_date, is_active) VALUES (?,?,?,?)");
            ps.setString(1, InputValidator.clean(gs(body, "name"))); ps.setString(2, gs(body, "startDate"));
            ps.setString(3, gs(body, "endDate")); ps.setBoolean(4, body.has("isActive") && body.get("isActive").getAsBoolean());
            ps.executeUpdate(); ps.close();
            HttpUtil.sendSuccess(ex, "Tahun akademik berhasil ditambahkan", null);
        }
    }

    private String gs(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
}
