package id.kamsib.antaboga.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serves JNLP file for Java Web Start launcher.
 * GET /launch/antaboga.jnlp → triggers native Java desktop app
 */
public class JnlpHandler implements HttpHandler {

    private final int port;

    public JnlpHandler(int port) {
        this.port = port;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (path.endsWith(".jnlp")) {
            serveJnlp(exchange);
        } else if (path.endsWith(".jar")) {
            serveJar(exchange);
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void serveJnlp(HttpExchange exchange) throws IOException {
        String host = exchange.getRequestHeaders().getFirst("Host");
        if (host == null) host = "127.0.0.1:" + port;
        String codebase = "http://" + host + "/launch";

        String jnlp = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<jnlp spec=\"1.0+\" codebase=\"" + codebase + "\" href=\"antaboga.jnlp\">\n"
            + "  <information>\n"
            + "    <title>Antaboga ERP — Desktop Report</title>\n"
            + "    <vendor>Kamsib</vendor>\n"
            + "    <description>Antaboga ERP Desktop Report Module</description>\n"
            + "    <offline-allowed/>\n"
            + "  </information>\n"
            + "  <security>\n"
            + "    <all-permissions/>\n"
            + "  </security>\n"
            + "  <resources>\n"
            + "    <j2se version=\"1.8+\" />\n"
            + "    <jar href=\"antaboga-desktop.jar\" main=\"true\" />\n"
            + "  </resources>\n"
            + "  <application-desc main-class=\"id.kamsib.antaboga.desktop.AntabogaDesktop\" />\n"
            + "</jnlp>\n";

        byte[] bytes = jnlp.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/x-java-jnlp-file");
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private void serveJar(HttpExchange exchange) throws IOException {
        // Serve the main JAR file for JNLP download
        java.io.File jarFile = new java.io.File("target/antaboga-1.2.0.jar");
        if (!jarFile.exists()) {
            // Fallback: try without version
            jarFile = new java.io.File("target/antaboga-1.0.0.jar");
        }
        // Also try the running jar
        if (!jarFile.exists()) {
            String jarPath = JnlpHandler.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath();
            jarFile = new java.io.File(jarPath);
        }

        if (!jarFile.exists()) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        byte[] data = java.nio.file.Files.readAllBytes(jarFile.toPath());
        exchange.getResponseHeaders().set("Content-Type", "application/java-archive");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"antaboga-desktop.jar\"");
        exchange.sendResponseHeaders(200, data.length);
        OutputStream os = exchange.getResponseBody();
        os.write(data);
        os.close();
    }
}
