package id.kamsib.antaboga.server;

import com.sun.net.httpserver.HttpServer;
import id.kamsib.antaboga.config.AppConfig;
import id.kamsib.antaboga.db.DatabaseManager;
import id.kamsib.antaboga.modules.academic.AcademicHandler;
import id.kamsib.antaboga.modules.attendance.AttendanceHandler;
import id.kamsib.antaboga.modules.auth.AuthHandler;
import id.kamsib.antaboga.modules.dashboard.DashboardHandler;
import id.kamsib.antaboga.modules.finance.FinanceHandler;
import id.kamsib.antaboga.modules.student.StudentHandler;
import id.kamsib.antaboga.modules.teacher.TeacherHandler;
import id.kamsib.antaboga.security.SessionManager;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Embedded HTTP server for Antaboga ERP.
 */
public class WebServer {
    private final AppConfig config;
    private final DatabaseManager db;
    private HttpServer server;

    public WebServer(AppConfig config, DatabaseManager db) {
        this.config = config;
        this.db = db;
    }

    public void start() throws IOException {
        SessionManager sessions = new SessionManager(
            db.getConnection(), config.getSessionTimeout(), config.getRateLimit());

        server = HttpServer.create(
            new InetSocketAddress(config.getHost(), config.getPort()), 0);

        // API routes
        AuthHandler auth = new AuthHandler(db.getConnection(), sessions);
        server.createContext("/api/auth", auth);
        server.createContext("/api/dashboard", new DashboardHandler(db.getConnection(), sessions));
        server.createContext("/api/students", new StudentHandler(db.getConnection(), sessions));
        server.createContext("/api/teachers", new TeacherHandler(db.getConnection(), sessions));
        server.createContext("/api/academic", new AcademicHandler(db.getConnection(), sessions));
        server.createContext("/api/finance", new FinanceHandler(db.getConnection(), sessions));
        server.createContext("/api/attendance", new AttendanceHandler(db.getConnection(), sessions));

        // OA_HTML JSP-style pages (rendered by Java)
        server.createContext("/OA_HTML", new OAHtmlHandler(db.getConnection(), sessions));

        // JNLP launcher for Java Web Start desktop app
        server.createContext("/launch", new JnlpHandler(config.getPort()));

        // Static files (must be last - catches all other routes)
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();

        // Schedule expired session cleanup every 10 minutes
        java.util.concurrent.ScheduledExecutorService scheduler =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(sessions::cleanExpiredSessions, 10, 10, java.util.concurrent.TimeUnit.MINUTES);
    }

    public void stop() {
        if (server != null) {
            server.stop(2);
            System.out.println("  [OK] Server stopped");
        }
    }
}
