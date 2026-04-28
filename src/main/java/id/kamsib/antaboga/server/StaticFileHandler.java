package id.kamsib.antaboga.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Serves static web files from classpath resources.
 */
public class StaticFileHandler implements HttpHandler {

    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("css", "text/css; charset=UTF-8");
        MIME_TYPES.put("js", "application/javascript; charset=UTF-8");
        MIME_TYPES.put("json", "application/json; charset=UTF-8");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("woff2", "font/woff2");
        MIME_TYPES.put("woff", "font/woff");
        MIME_TYPES.put("ttf", "font/ttf");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Default to index.html
        if (path.equals("/") || path.equals("")) {
            path = "/index.html";
        }

        // Security: prevent directory traversal
        if (path.contains("..") || path.contains("\\")) {
            exchange.sendResponseHeaders(403, -1);
            return;
        }

        String resourcePath = "/web" + path;
        InputStream is = getClass().getResourceAsStream(resourcePath);

        if (is == null) {
            // SPA fallback: serve index.html for non-API routes
            if (!path.startsWith("/api/")) {
                is = getClass().getResourceAsStream("/web/index.html");
                if (is == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                path = "/index.html";
            } else {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
        }

        String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
        String mime = MIME_TYPES.getOrDefault(ext, "application/octet-stream");

        byte[] data = readAllBytes(is);
        is.close();

        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
        exchange.sendResponseHeaders(200, data.length);
        OutputStream os = exchange.getResponseBody();
        os.write(data);
        os.close();
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int len;
        while ((len = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, len);
        }
        return buffer.toByteArray();
    }
}
