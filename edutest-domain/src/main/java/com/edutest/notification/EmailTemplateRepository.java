package com.edutest.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads email templates from JSON at startup.
 *
 * Default location: classpath:email-templates/templates.json. Can be overridden via
 * {@code app.email.templates-location} for per-environment customization.
 *
 * Templates are immutable after load — not hot-reloaded. Restart to pick up changes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailTemplateRepository {

    private final ObjectMapper objectMapper;

    @Value("${app.email.templates-location:classpath:email-templates/templates.json}")
    private Resource templatesResource;

    private final Map<String, EmailTemplate> templates = new HashMap<>();

    @PostConstruct
    void load() throws IOException {
        try (InputStream in = templatesResource.getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            JsonNode tplNode = root.path("templates");
            if (!tplNode.isObject()) {
                throw new IllegalStateException(
                        "Email templates JSON must have a top-level 'templates' object");
            }

            tplNode.fields().forEachRemaining(entry -> {
                String name = entry.getKey();
                JsonNode node = entry.getValue();
                String subject = node.path("subject").asText("");
                List<String> body = new ArrayList<>();
                JsonNode bodyNode = node.path("body");
                if (bodyNode.isArray()) {
                    bodyNode.forEach(line -> body.add(line.asText("")));
                } else if (bodyNode.isTextual()) {
                    body.add(bodyNode.asText());
                }
                templates.put(name, new EmailTemplate(subject, List.copyOf(body)));
            });

            log.info("Loaded {} email templates from {}", templates.size(),
                    templatesResource.getDescription());
        }
    }

    public EmailTemplate get(String name) {
        EmailTemplate tpl = templates.get(name);
        if (tpl == null) {
            throw new IllegalArgumentException("No email template named: " + name);
        }
        return tpl;
    }
}
