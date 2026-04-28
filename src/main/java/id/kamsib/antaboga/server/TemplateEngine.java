package id.kamsib.antaboga.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple JSP-like template engine.
 * Replaces ${variable} placeholders with values from a context map.
 * Supports basic <%-- comments --%> stripping.
 * 
 * Templates are loaded from classpath /oa_html/ directory.
 */
public class TemplateEngine {

    private static final String TEMPLATE_DIR = "/oa_html/";

    /**
     * Render a JSP template with the given context variables.
     */
    public static String render(String templateName, Map<String, String> context) throws IOException {
        String template = loadTemplate(templateName);
        if (template == null) {
            throw new FileNotFoundException("Template not found: " + templateName);
        }

        // Strip JSP comments <%-- ... --%>
        template = template.replaceAll("(?s)<%--.*?--%>", "");

        // Add built-in variables
        if (context == null) {
            context = new HashMap<>();
        }
        context.putIfAbsent("appName", "Antaboga ERP");
        context.putIfAbsent("appVersion", "1.2.0");
        context.putIfAbsent("year", String.valueOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)));
        context.putIfAbsent("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        context.putIfAbsent("serverInfo", "Antaboga/1.2.0 (Java " + System.getProperty("java.version") + ")");
        context.putIfAbsent("contextPath", "/OA_HTML");
        context.putIfAbsent("errorMessage", "");
        context.putIfAbsent("successMessage", "");

        // Replace ${variable} placeholders
        for (Map.Entry<String, String> entry : context.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            template = template.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
        }

        // Inject favicon into <head>
        template = template.replace("<head>",
            "<head>\n    <link rel=\"icon\" type=\"image/svg+xml\" href=\"/favicon.svg\">");

        return template;
    }

    /**
     * Load a template file from classpath.
     */
    private static String loadTemplate(String name) throws IOException {
        InputStream is = TemplateEngine.class.getResourceAsStream(TEMPLATE_DIR + name);
        if (is == null) return null;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int len;
        while ((len = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, len);
        }
        is.close();
        return buffer.toString("UTF-8");
    }

    /**
     * Create a new mutable context map with common defaults.
     */
    public static Map<String, String> newContext() {
        return new HashMap<>();
    }
}
