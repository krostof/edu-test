package com.edutest.webserver.service.email;

import com.edutest.service.email.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * SMTP-backed implementation of {@link EmailService}.
 *
 * Active when {@code app.mail.enabled=true} (default true). When SMTP is misconfigured
 * or unreachable, errors are logged but not thrown — password reset tokens are still
 * issued and the user can retry. For dev environments without SMTP, see
 * {@link LoggingEmailService} which is active when {@code app.mail.enabled=false}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true", matchIfMissing = true)
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:noreply@edutest.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Sent email to {} (subject: {})", to, subject);
        } catch (MailException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage(), e);
        }
    }
}
