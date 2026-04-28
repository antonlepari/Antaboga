package id.kamsib.antaboga.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for handling HTTP requests and responses.
 */
public class HttpUtil {
    private static final Gson gson = new Gson();

    public static void sendJson(HttpExchange exchange, int status, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("X-XSS-Protection", "1; mode=block");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    public static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("error", true);
        obj.addProperty("message", message);
        sendJson(exchange, status, obj);
    }

    public static void sendSuccess(HttpExchange exchange, String message, Object data) throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("message", message);
        if (data != null) {
            obj.add("data", gson.toJsonTree(data));
        }
        sendJson(exchange, 200, obj);
    }

    public static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            try {
                String key = URLDecoder.decode(kv[0], "UTF-8");
                String val = kv.length > 1 ? URLDecoder.decode(kv[1], "UTF-8") : "";
                params.put(key, val);
            } catch (UnsupportedEncodingException e) {
                // UTF-8 always supported
            }
        }
        return params;
    }

    public static JsonObject parseJsonBody(HttpExchange exchange) throws IOException {
        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        return gson.fromJson(reader, JsonObject.class);
    }

    public static String getCookie(HttpExchange exchange, String name) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) return null;
        for (String cookie : cookieHeader.split(";")) {
            String[] kv = cookie.trim().split("=", 2);
            if (kv[0].equals(name) && kv.length > 1) {
                return kv[1];
            }
        }
        return null;
    }

    public static void setSessionCookie(HttpExchange exchange, String sessionId, int maxAge) {
        String cookie = "ANTABOGA_SESSION=" + sessionId
            + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + maxAge;
        exchange.getResponseHeaders().add("Set-Cookie", cookie);
    }

    public static String getClientIp(HttpExchange exchange) {
        String forwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    public static String getPathSegment(String path, int index) {
        String[] parts = path.split("/");
        // parts[0] is empty (leading slash), parts[1] is "api", parts[2] is resource, etc.
        int actual = index + 1; // offset for leading slash
        if (actual < parts.length) {
            return parts[actual];
        }
        return null;
    }
}
