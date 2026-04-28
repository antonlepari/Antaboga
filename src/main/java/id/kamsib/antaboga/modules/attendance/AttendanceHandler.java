package id.kamsib.antaboga.modules.attendance;

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

public class AttendanceHandler implements HttpHandler {
    private final Connection db;
    private final SessionManager sessions;

    public AttendanceHandler(Connection db, SessionManager sessions) {
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
            if ("GET".equals(method)) listAttendance(exchange);
            else if ("POST".equals(method)) recordAttendance(exchange, userId);
            else HttpUtil.sendError(exchange, 405, "Method tidak diizinkan");
        } catch (Exception e) {
            HttpUtil.sendError(exchange, 500, "Terjadi kesalahan server");
        }
    }

    private void listAttendance(HttpExchange ex) throws Exception {
        Map<String, String> params = HttpUtil.parseQuery(ex.getRequestURI().getQuery());
        String date = params.getOrDefault("date", "CURRENT_DATE");
        String classId = params.get("classId");

        StringBuilder sql = new StringBuilder(
            "SELECT a.*, s.full_name, s.nis FROM attendance a JOIN students s ON a.student_id = s.id WHERE 1=1");
        if (classId != null) sql.append(" AND a.class_id = ?");
        if (!"CURRENT_DATE".equals(date)) sql.append(" AND a.att_date = ?");
        sql.append(" ORDER BY s.full_name");

        PreparedStatement ps = db.prepareStatement(sql.toString());
        int idx = 1;
        if (classId != null) ps.setInt(idx++, InputValidator.parseIntSafe(classId, 0));
        if (!"CURRENT_DATE".equals(date)) ps.setString(idx++, date.replaceAll("[^0-9-]", ""));
        ResultSet rs = ps.executeQuery();

        JsonArray arr = new JsonArray();
        while (rs.next()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", rs.getInt("id"));
            o.addProperty("studentName", rs.getString("full_name"));
            o.addProperty("nis", rs.getString("nis"));
            o.addProperty("date", rs.getString("att_date"));
            o.addProperty("status", rs.getString("status"));
            o.addProperty("remarks", rs.getString("remarks"));
            arr.add(o);
        }
        rs.close();
        ps.close();
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.add("data", arr);
        HttpUtil.sendJson(ex, 200, resp);
    }

    private void recordAttendance(HttpExchange ex, int userId) throws Exception {
        JsonObject body = HttpUtil.parseJsonBody(ex);
        PreparedStatement ps = db.prepareStatement(
            "INSERT INTO attendance (student_id, class_id, att_date, status, remarks, recorded_by) VALUES (?,?,?,?,?,?)");
        ps.setInt(1, body.get("studentId").getAsInt());
        ps.setInt(2, body.get("classId").getAsInt());
        ps.setString(3, body.has("date") ? body.get("date").getAsString() : new java.sql.Date(System.currentTimeMillis()).toString());
        ps.setString(4, body.get("status").getAsString());
        ps.setString(5, body.has("remarks") ? body.get("remarks").getAsString() : "");
        ps.setInt(6, userId);
        ps.executeUpdate();
        ps.close();
        HttpUtil.sendSuccess(ex, "Absensi berhasil dicatat", null);
    }
}
