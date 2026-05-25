package com.seu.seustock.configuration;

import jakarta.servlet.http.HttpServletResponse;

public final class HtmxResponse {

    private HtmxResponse() {
    }

    public static void success(HttpServletResponse response, String message) {
        toast(response, "success", message);
    }

    public static void error(HttpServletResponse response, String message) {
        toast(response, "error", message);
    }

    private static void toast(HttpServletResponse response, String type, String message) {
        response.addHeader("HX-Trigger", "{\"app:toast\":{\"type\":\""
                + escapeJson(type)
                + "\",\"message\":\""
                + escapeJson(message)
                + "\"}}");
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
