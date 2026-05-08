package com.edutest.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTemplateRepositoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Loads templates from JSON with array body")
    void loadsArrayBody() throws Exception {
        String json = """
                {
                  "templates": {
                    "test-tpl": {
                      "subject": "Hi {{name}}",
                      "body": ["Line 1 {{name}}", "Line 2"]
                    }
                  }
                }
                """;
        EmailTemplateRepository repo = newRepoWith(json);
        EmailTemplate tpl = repo.get("test-tpl");

        assertThat(tpl.subject()).isEqualTo("Hi {{name}}");
        assertThat(tpl.body()).containsExactly("Line 1 {{name}}", "Line 2");
        assertThat(tpl.renderBody(Map.of("name", "Bob"))).isEqualTo("Line 1 Bob\nLine 2");
    }

    @Test
    @DisplayName("Loads templates with string body (single line)")
    void loadsStringBody() throws Exception {
        String json = """
                {
                  "templates": {
                    "single": {
                      "subject": "Subj",
                      "body": "One line {{x}}"
                    }
                  }
                }
                """;
        EmailTemplateRepository repo = newRepoWith(json);

        assertThat(repo.get("single").renderBody(Map.of("x", "42"))).isEqualTo("One line 42");
    }

    @Test
    @DisplayName("Throws on unknown template name")
    void throwsOnUnknown() throws Exception {
        String json = """
                { "templates": { "a": { "subject": "S", "body": [] } } }
                """;
        EmailTemplateRepository repo = newRepoWith(json);

        assertThatThrownBy(() -> repo.get("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    @DisplayName("Throws when JSON has no 'templates' object")
    void throwsOnMalformedJson() throws Exception {
        String json = """
                { "wrong": "shape" }
                """;
        assertThatThrownBy(() -> newRepoWith(json))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("templates");
    }

    /** Loads the repo with an in-memory JSON resource, simulating the @PostConstruct path. */
    private EmailTemplateRepository newRepoWith(String json) throws Exception {
        EmailTemplateRepository repo = new EmailTemplateRepository(objectMapper);
        ReflectionTestUtils.setField(repo, "templatesResource",
                new ByteArrayResource(json.getBytes()));
        ReflectionTestUtils.invokeMethod(repo, "load");
        return repo;
    }
}
