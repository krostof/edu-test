package com.edutest.webserver.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * Bound to specific URL patterns via {@link RateLimitConfig}, with the rule name
 * carried per-instance.
 *
 * Client key strategy:
 *  - Authenticated: principal name (username).
 *  - Anonymous: client IP — useful for /login, /forgot-password, /register
 *    where there's no auth yet.
 *
 * On block: responds 429 with JSON {error, message, retryAfterSeconds} and
 * sets the standard {@code Retry-After} header.
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final String ruleName;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String clientKey = resolveClientKey(request);
        RateLimitService.ConsumeResult result = rateLimitService.tryConsume(ruleName, clientKey);

        if (result.allowed()) {
            return true;
        }

        log.warn("Rate limit hit: rule={} clientKey={} retryAfter={}s",
                ruleName, clientKey, result.retryAfterSeconds());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "error", "Too Many Requests",
                "message", "Zbyt wiele żądań — spróbuj ponownie za " + result.retryAfterSeconds() + " s",
                "retryAfterSeconds", result.retryAfterSeconds()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }

    private static String resolveClientKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            return "user:" + auth.getName();
        }
        // Prefer X-Forwarded-For first IP if set (LB/proxy), else remote addr
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            int comma = fwd.indexOf(',');
            return "ip:" + (comma > 0 ? fwd.substring(0, comma).trim() : fwd.trim());
        }
        return "ip:" + request.getRemoteAddr();
    }
}
