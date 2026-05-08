package com.edutest.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateTest {

    @Test
    @DisplayName("Interpolates {{var}} placeholders in subject and body")
    void interpolatesPlaceholders() {
        EmailTemplate tpl = new EmailTemplate(
                "Hello {{name}}",
                List.of("Cześć {{name}},", "", "Twój wynik: {{score}}/{{max}}.")
        );

        Map<String, String> vars = Map.of("name", "Alice", "score", "8", "max", "10");

        assertThat(tpl.renderSubject(vars)).isEqualTo("Hello Alice");
        assertThat(tpl.renderBody(vars))
                .isEqualTo("Cześć Alice,\n\nTwój wynik: 8/10.");
    }

    @Test
    @DisplayName("Missing variable renders as empty (no exception)")
    void missingVarRendersEmpty() {
        EmailTemplate tpl = new EmailTemplate("S", List.of("Hi {{firstName}} {{lastName}}"));
        Map<String, String> vars = Map.of("firstName", "Bob");

        assertThat(tpl.renderBody(vars)).isEqualTo("Hi Bob {{lastName}}");
    }

    @Test
    @DisplayName("Null value renders as empty string")
    void nullValueRendersEmpty() {
        EmailTemplate tpl = new EmailTemplate("S", List.of("[{{x}}]"));
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("x", null);

        assertThat(tpl.renderBody(vars)).isEqualTo("[]");
    }

    @Test
    @DisplayName("Body lines joined with \\n preserving blank lines")
    void joinsBodyLines() {
        EmailTemplate tpl = new EmailTemplate("S", List.of("Line 1", "", "Line 3"));
        assertThat(tpl.renderBody(Map.of())).isEqualTo("Line 1\n\nLine 3");
    }

    @Test
    @DisplayName("Single-line body has no trailing newline")
    void singleLine() {
        EmailTemplate tpl = new EmailTemplate("S", List.of("Only line"));
        assertThat(tpl.renderBody(Map.of())).isEqualTo("Only line");
    }
}
