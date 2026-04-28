package id.kamsib.antaboga.modules.dashboard;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import id.kamsib.antaboga.security.SessionManager;
import id.kamsib.antaboga.server.HttpUtil;

import java.io.IOException;
import java.sql.*;

public class DashboardHandler implements HttpHandler {
    private final Connection db;
    private final SessionManager sessions;

    public DashboardHandler(Connection db, SessionManager sessions) {
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

            if (!"GET".equals(exchange.getRequestMethod())) {
                HttpUtil.sendError(exchange, 405, "Method tidak diizinkan");
                return;
            }

            JsonObject stats = new JsonObject();
            stats.addProperty("totalStudents", countTable("students"));
            stats.addProperty("totalTeachers", countTable("teachers"));
            stats.addProperty("totalClasses", countTable("classes"));
            stats.addProperty("totalSubjects", countTable("subjects"));
            stats.addProperty("unpaidInvoices", countWhere("invoices", "status = 'unpaid'"));
            stats.addProperty("activeYear", getActiveYear());
            stats.addProperty("todayAttendance", getTodayAttendanceRate());

            JsonObject resp = new JsonObject();
            resp.addProperty("success", true);
            resp.add("stats", stats);
            HttpUtil.sendJson(exchange, 200, resp);

        } catch (Exception e) {
            System.err.println("[DASHBOARD ERROR] " + e.getMessage());
            HttpUtil.sendError(exchange, 500, "Terjadi kesalahan server");
        }
    }

    private int countTable(String table) throws SQLException {
        Statement s = db.createStatement();
        ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table);
        rs.next();
        int count = rs.getInt(1);
        rs.close(); s.close();
        return count;
    }

    private int countWhere(String table, String where) throws SQLException {
        Statement s = db.createStatement();
        ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + where);
        rs.next();
        int count = rs.getInt(1);
        rs.close(); s.close();
        return count;
    }

    private String getActiveYear() throws SQLException {
        Statement s = db.createStatement();
        ResultSet rs = s.executeQuery("SELECT name FROM academic_years WHERE is_active = TRUE LIMIT 1");
        String name = rs.next() ? rs.getString("name") : "-";
        rs.close(); s.close();
        return name;
    }

    private double getTodayAttendanceRate() throws SQLException {
        Statement s = db.createStatement();
        ResultSet rs = s.executeQuery(
            "SELECT COUNT(*) as total, SUM(CASE WHEN status='hadir' THEN 1 ELSE 0 END) as present "
            + "FROM attendance WHERE att_date = CURRENT_DATE");
        double rate = 0;
        if (rs.next()) {
            int total = rs.getInt("total");
            if (total > 0) rate = (rs.getDouble("present") / total) * 100;
        }
        rs.close(); s.close();
        return Math.round(rate * 10.0) / 10.0;
    }
}
