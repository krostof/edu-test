package com.edutest.service.email;

/**
 * Single seam between domain services and the SMTP implementation in web-server.
 *
 * Implementations should be resilient — failure to send must not block the caller's
 * transaction (password reset shouldn't fail just because SMTP is down).
 */
public interface EmailService {

    /**
     * Send a plain-text email. Implementation should log+swallow exceptions; callers
     * shouldn't have to wrap this in try/catch for the happy path.
     *
     * @param to recipient email address
     * @param subject subject line
     * @param body plain text body
     */
    void send(String to, String subject, String body);
}
