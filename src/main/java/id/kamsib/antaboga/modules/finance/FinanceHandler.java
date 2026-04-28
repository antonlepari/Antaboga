package id.kamsib.antaboga.modules.finance;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import id.kamsib.antaboga.security.InputValidator;
import id.kamsib.antaboga.security.SessionManager;
import id.kamsib.antaboga.server.HttpUtil;

import java.io.IOException;
import java.sql.*;

public class FinanceHandler implements HttpHandler {
    private final Connection db;
    private final SessionManager sessions;

    public FinanceHandler(Connection db, SessionManager sessions) {
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
            if (path.contains("/invoices")) handleInvoices(exchange, method, userId);
            else if (path.contains("/payments")) handlePayments(exchange, method, userId);
            else if (path.contains("/fee-types")) handleFeeTypes(exchange, method);
            else if (path.contains("/summary")) handleSummary(exchange);
            else HttpUtil.sendError(exchange, 404, "Endpoint tidak ditemukan");
        } catch (Exception e) {
            HttpUtil.sendError(exchange, 500, "Terjadi kesalahan server");
        }
    }

    private void handleInvoices(HttpExchange ex, String method, int userId) throws Exception {
        if ("GET".equals(method)) {
            PreparedStatement ps = db.prepareStatement(
                "SELECT i.*, s.full_name as student_name, f.name as fee_name FROM invoices i "
                + "JOIN students s ON i.student_id = s.id JOIN fee_types f ON i.fee_type_id = f.id ORDER BY i.created_at DESC LIMIT 100");
            ResultSet rs = ps.executeQuery();
            JsonArray arr = new JsonArray();
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", rs.getInt("id")); o.addProperty("invoiceNumber", rs.getString("invoice_number"));
                o.addProperty("studentName", rs.getString("student_name")); o.addProperty("feeName", rs.getString("fee_name"));
                o.addProperty("amount", rs.getDouble("amount")); o.addProperty("dueDate", rs.getString("due_date"));
                o.addProperty("status", rs.getString("status")); arr.add(o);
            }
            rs.close(); ps.close();
            JsonObject resp = new JsonObject(); resp.addProperty("success", true); resp.add("data", arr);
            HttpUtil.sendJson(ex, 200, resp);
        } else if ("POST".equals(method)) {
            JsonObject body = HttpUtil.parseJsonBody(ex);
            String invNum = "INV-" + System.currentTimeMillis();
            PreparedStatement ps = db.prepareStatement(
                "INSERT INTO invoices (invoice_number, student_id, fee_type_id, amount, due_date) VALUES (?,?,?,?,?)");
            ps.setString(1, invNum); ps.setInt(2, body.get("studentId").getAsInt());
            ps.setInt(3, body.get("feeTypeId").getAsInt()); ps.setDouble(4, body.get("amount").getAsDouble());
            ps.setString(5, gs(body, "dueDate"));
            ps.executeUpdate(); ps.close();
            HttpUtil.sendSuccess(ex, "Invoice berhasil dibuat: " + invNum, null);
        }
    }

    private void handlePayments(HttpExchange ex, String method, int userId) throws Exception {
        if ("POST".equals(method)) {
            JsonObject body = HttpUtil.parseJsonBody(ex);
            int invoiceId = body.get("invoiceId").getAsInt();
            PreparedStatement ps = db.prepareStatement(
                "INSERT INTO payments (invoice_id, amount, payment_method, received_by, notes) VALUES (?,?,?,?,?)");
            ps.setInt(1, invoiceId); ps.setDouble(2, body.get("amount").getAsDouble());
            ps.setString(3, gs(body, "paymentMethod")); ps.setInt(4, userId); ps.setString(5, gs(body, "notes"));
            ps.executeUpdate(); ps.close();
            // Update invoice status
            PreparedStatement upd = db.prepareStatement("UPDATE invoices SET status = 'paid' WHERE id = ?");
            upd.setInt(1, invoiceId); upd.executeUpdate(); upd.close();
            HttpUtil.sendSuccess(ex, "Pembayaran berhasil dicatat", null);
        } else if ("GET".equals(method)) {
            ResultSet rs = db.createStatement().executeQuery(
                "SELECT p.*, i.invoice_number FROM payments p JOIN invoices i ON p.invoice_id = i.id ORDER BY p.payment_date DESC LIMIT 100");
            JsonArray arr = new JsonArray();
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", rs.getInt("id")); o.addProperty("invoiceNumber", rs.getString("invoice_number"));
                o.addProperty("amount", rs.getDouble("amount")); o.addProperty("paymentMethod", rs.getString("payment_method"));
                o.addProperty("paymentDate", rs.getString("payment_date")); arr.add(o);
            }
            rs.close();
            JsonObject resp = new JsonObject(); resp.addProperty("success", true); resp.add("data", arr);
            HttpUtil.sendJson(ex, 200, resp);
        }
    }

    private void handleFeeTypes(HttpExchange ex, String method) throws Exception {
        if ("GET".equals(method)) {
            ResultSet rs = db.createStatement().executeQuery("SELECT * FROM fee_types ORDER BY name");
            JsonArray arr = new JsonArray();
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", rs.getInt("id")); o.addProperty("name", rs.getString("name"));
                o.addProperty("amount", rs.getDouble("amount")); o.addProperty("description", rs.getString("description")); arr.add(o);
            }
            rs.close();
            JsonObject resp = new JsonObject(); resp.addProperty("success", true); resp.add("data", arr);
            HttpUtil.sendJson(ex, 200, resp);
        } else if ("POST".equals(method)) {
            JsonObject body = HttpUtil.parseJsonBody(ex);
            PreparedStatement ps = db.prepareStatement("INSERT INTO fee_types (name, amount, description) VALUES (?,?,?)");
            ps.setString(1, InputValidator.clean(gs(body, "name"))); ps.setDouble(2, body.get("amount").getAsDouble());
            ps.setString(3, gs(body, "description")); ps.executeUpdate(); ps.close();
            HttpUtil.sendSuccess(ex, "Jenis biaya berhasil ditambahkan", null);
        }
    }

    private void handleSummary(HttpExchange ex) throws Exception {
        Statement s = db.createStatement();
        ResultSet rs = s.executeQuery("SELECT COALESCE(SUM(amount),0) as total FROM payments");
        rs.next(); double totalPaid = rs.getDouble("total"); rs.close();
        rs = s.executeQuery("SELECT COALESCE(SUM(amount),0) as total FROM invoices WHERE status = 'unpaid'");
        rs.next(); double totalUnpaid = rs.getDouble("total"); rs.close();
        s.close();
        JsonObject data = new JsonObject();
        data.addProperty("totalPaid", totalPaid); data.addProperty("totalUnpaid", totalUnpaid);
        JsonObject resp = new JsonObject(); resp.addProperty("success", true); resp.add("data", data);
        HttpUtil.sendJson(ex, 200, resp);
    }

    private String gs(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
}
