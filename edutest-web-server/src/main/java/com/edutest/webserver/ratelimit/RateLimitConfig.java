package com.edutest.webserver.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Maps rate-limit rules to URL patterns.
 *
 * Each entry creates a fresh interceptor instance bound to one rule, registered
 * for one or more paths. Order matters only for overlapping patterns — we don't
 * have those here.
 */
@Configuration
@RequiredArgsConstructor
public class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Docker-backed code execution — most expensive
        registry.addInterceptor(new RateLimitInterceptor("run-code", rateLimitService, objectMapper))
                .addPathPatterns("/api/tests/*/attempts/*/answers/*/run");

        // Status polling — cheap but uncapped polling could DoS
        registry.addInterceptor(new RateLimitInterceptor("run-status", rateLimitService, objectMapper))
                .addPathPatterns("/api/tests/*/attempts/*/answers/*/run-status");

        // Brute-force protection on auth endpoints
        registry.addInterceptor(new RateLimitInterceptor("login", rateLimitService, objectMapper))
                .addPathPatterns("/api/auth/login");

        registry.addInterceptor(new RateLimitInterceptor("forgot-password", rateLimitService, objectMapper))
                .addPathPatterns("/api/auth/forgot-password");

        registry.addInterceptor(new RateLimitInterceptor("register", rateLimitService, objectMapper))
                .addPathPatterns("/api/auth/register");
    }
}
