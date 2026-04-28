package id.kamsib.antaboga.security;

import java.security.SecureRandom;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session management with CSRF protection.
 */
public class SessionManager {
    private final Connection db;
    private final int timeoutSeconds;
    private final SecureRandom random = new SecureRandom();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    // Rate limiting: IP -> request timestamps
    private final Map<String, long[]> rateLimitMap = new ConcurrentHashMap<>();
    private final int rateLimit;

    public SessionManager(Connection db, int timeoutSeconds, int rateLimit) {
        this.db = db;
        this.timeoutSeconds = timeoutSeconds;
        this.rateLimit = rateLimit;
    }

    public String createSession(int userId) throws SQLException {
        String sessionId = generateToken(48);
        String csrfToken = generateToken(32);

        // Clean old sessions for this user
        PreparedStatement del = db.prepareStatement("DELETE FROM sessions WHERE user_id = ?");
        del.setInt(1, userId);
        del.executeUpdate();
        del.close();

        PreparedStatement ps = db.prepareStatement(
            "INSERT INTO sessions (id, user_id, csrf_token, expires_at) VALUES (?, ?, ?, ?)");
        ps.setString(1, sessionId);
        ps.setInt(2, userId);
        ps.setString(3, csrfToken);
        ps.setTimestamp(4, new Timestamp(System.currentTimeMillis() + (timeoutSeconds * 1000L)));
        ps.executeUpdate();
        ps.close();

        return sessionId;
    }

    /**
     * Validates a session and returns user_id, or -1 if invalid.
     */
    public int validateSession(String sessionId) throws SQLException {
        if (sessionId == null || sessionId.isEmpty()) return -1;

        PreparedStatement ps = db.prepareStatement(
            "SELECT user_id, expires_at FROM sessions WHERE id = ?");
        ps.setString(1, sessionId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Timestamp expires = rs.getTimestamp("expires_at");
            if (expires.after(new Timestamp(System.currentTimeMillis()))) {
                int userId = rs.getInt("user_id");
                rs.close();
                ps.close();

                // Extend session
                PreparedStatement ext = db.prepareStatement(
                    "UPDATE sessions SET expires_at = ? WHERE id = ?");
                ext.setTimestamp(1, new Timestamp(System.currentTimeMillis() + (timeoutSeconds * 1000L)));
                ext.setString(2, sessionId);
                ext.executeUpdate();
                ext.close();

                return userId;
            }
        }

        rs.close();
        ps.close();
        return -1;
    }

    public String getCsrfToken(String sessionId) throws SQLException {
        PreparedStatement ps = db.prepareStatement("SELECT csrf_token FROM sessions WHERE id = ?");
        ps.setString(1, sessionId);
        ResultSet rs = ps.executeQuery();
        String token = rs.next() ? rs.getString("csrf_token") : null;
        rs.close();
        ps.close();
        return token;
    }

    public void destroySession(String sessionId) throws SQLException {
        PreparedStatement ps = db.prepareStatement("DELETE FROM sessions WHERE id = ?");
        ps.setString(1, sessionId);
        ps.executeUpdate();
        ps.close();
    }

    /**
     * Rate limiting check. Returns true if request is allowed.
     */
    public boolean checkRateLimit(String ip) {
        long now = System.currentTimeMillis();
        long window = 60_000; // 1 minute

        long[] timestamps = rateLimitMap.computeIfAbsent(ip, k -> new long[0]);

        // Count requests in window
        int count = 0;
        for (long t : timestamps) {
            if (now - t < window) count++;
        }

        if (count >= rateLimit) return false;

        // Add current timestamp
        long[] newTs = new long[Math.min(timestamps.length + 1, rateLimit + 10)];
        int idx = 0;
        for (long t : timestamps) {
            if (now - t < window && idx < newTs.length - 1) {
                newTs[idx++] = t;
            }
        }
        newTs[idx] = now;
        rateLimitMap.put(ip, java.util.Arrays.copyOf(newTs, idx + 1));

        return true;
    }

    /**
     * Remove expired sessions from the database.
     * Call periodically to prevent table bloat.
     */
    public void cleanExpiredSessions() {
        try {
            PreparedStatement ps = db.prepareStatement(
                "DELETE FROM sessions WHERE expires_at < CURRENT_TIMESTAMP");
            int deleted = ps.executeUpdate();
            ps.close();
            if (deleted > 0) {
                System.out.println("  [SESSION] Cleaned " + deleted + " expired session(s)");
            }
        } catch (SQLException e) {
            System.err.println("[SESSION] Cleanup failed: " + e.getMessage());
        }
    }

    private String generateToken(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
