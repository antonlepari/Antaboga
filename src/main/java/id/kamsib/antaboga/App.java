package id.kamsib.antaboga;

import id.kamsib.antaboga.config.AppConfig;
import id.kamsib.antaboga.db.DatabaseManager;
import id.kamsib.antaboga.server.WebServer;

/**
 * Antaboga ERP - Education Resource Planning
 * Main entry point for the application.
 */
public class App {
    public static final String VERSION = "1.2.0";
    public static final String NAME = "Antaboga ERP";

    public static void main(String[] args) {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════╗");
        System.out.println("  ║   🐉 Antaboga ERP v" + VERSION + "            ║");
        System.out.println("  ║   Education Resource Planning        ║");
        System.out.println("  ╚══════════════════════════════════════╝");
        System.out.println();

        try {
            AppConfig config = new AppConfig();
            DatabaseManager db = new DatabaseManager(config);
            db.initialize();

            WebServer server = new WebServer(config, db);
            server.start();

            String url = "http://localhost:" + config.getPort();
            System.out.println("  [OK] Server running at " + url);
            System.out.println("  [OK] Press Ctrl+C to stop");
            System.out.println();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n  Shutting down Antaboga...");
                server.stop();
                db.close();
                System.out.println("  Goodbye! 🐉");
            }));

            // Keep main thread alive
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("  [ERROR] Failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
