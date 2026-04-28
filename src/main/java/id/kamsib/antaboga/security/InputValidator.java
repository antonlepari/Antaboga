package id.kamsib.antaboga.security;

import java.util.regex.Pattern;

/**
 * Input validation and sanitization utilities.
 */
public class InputValidator {

    private static final Pattern EMAIL = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern ALPHA_NUM = Pattern.compile("^[A-Za-z0-9_.-]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+\\-() ]{7,20}$");
    private static final Pattern NIS_NIP = Pattern.compile("^[A-Za-z0-9./-]{3,30}$");

    public static String sanitize(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                     .replace("<", "&lt;")
                     .replace(">", "&gt;")
                     .replace("\"", "&quot;")
                     .replace("'", "&#x27;");
    }

    public static String clean(String input) {
        if (input == null) return "";
        return input.trim();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE.matcher(phone).matches();
    }

    public static boolean isValidAlphaNum(String val) {
        return val != null && ALPHA_NUM.matcher(val).matches();
    }

    public static boolean isValidNisNip(String val) {
        return val != null && NIS_NIP.matcher(val).matches();
    }

    public static boolean isNotEmpty(String val) {
        return val != null && !val.trim().isEmpty();
    }

    public static boolean isValidLength(String val, int min, int max) {
        if (val == null) return min == 0;
        int len = val.trim().length();
        return len >= min && len <= max;
    }

    public static int parseIntSafe(String val, int defaultVal) {
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
