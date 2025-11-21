package com.example.hrcore.util;

import org.owasp.encoder.Encode;

public class SanitizationUtils {

    private SanitizationUtils() {
        // Utility class
    }

    public static String sanitizeForHtml(String input) {
        if (input == null) {
            return null;
        }
        return Encode.forHtml(input);
    }

    public static String sanitizeForJavaScript(String input) {
        if (input == null) {
            return null;
        }
        return Encode.forJavaScript(input);
    }

    public static String sanitizeForSql(String input) {
        if (input == null) {
            return null;
        }
        // Remove SQL injection patterns
        return input.replaceAll("['\"\\\\;]", "");
    }
}
