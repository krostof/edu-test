package com.edutest.webserver.service.email;

import com.edutest.service.email.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Dev fallback when no SMTP is configured. Activates when {@code app.mail.enabled=false}.
 * Logs the email to console at INFO level — useful for grabbing reset tokens during local dev.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false")
public class LoggingEmailService implements EmailService {

    @Override
    public void send(String to, String subject, String body) {
        log.info("=== EMAIL (logged, not sent) ===");
        log.info("  To: {}", to);
        log.info("  Subject: {}", subject);
        log.info("  Body:\n{}", body);
        log.info("=== END EMAIL ===");
    }
}
