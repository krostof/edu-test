package com.edutest.notification;

import java.util.List;

/**
 * Email template loaded from JSON. Body is stored as a list of lines for human-friendly
 * editing (no embedded \n in JSON strings).
 *
 * Both subject and body support {@code {{placeholder}}} interpolation.
 */
public record EmailTemplate(String subject, List<String> body) {

    public String renderSubject(java.util.Map<String, String> vars) {
        return interpolate(subject, vars);
    }

    public String renderBody(java.util.Map<String, String> vars) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < body.size(); i++) {
            sb.append(interpolate(body.get(i), vars));
            if (i < body.size() - 1) sb.append('\n');
        }
        return sb.toString();
    }

    private static String interpolate(String pattern, java.util.Map<String, String> vars) {
        String result = pattern;
        for (var e : vars.entrySet()) {
            String placeholder = "{{" + e.getKey() + "}}";
            String value = e.getValue() != null ? e.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
}
