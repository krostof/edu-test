package com.edutest.webserver.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-rule rate limit configuration.
 *
 * Each rule defines a request budget (capacity) refilled at a steady rate over
 * a window. e.g. {@code capacity=10, window-seconds=60} means "10 requests
 * per minute, replenished smoothly."
 *
 * Rules are referenced by name from {@link RateLimitConfig} interceptor mappings.
 */
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    /** Master switch — disable rate limiting entirely (e.g., for tests). */
    private boolean enabled = true;

    /** Named rules. Default values can be overridden per-environment. */
    private Map<String, Rule> rules = defaultRules();

    private static Map<String, Rule> defaultRules() {
        Map<String, Rule> r = new HashMap<>();
        // Docker-backed; expensive
        r.put("run-code", new Rule(10, 60));
        // Polling status — relatively cheap but should still cap
        r.put("run-status", new Rule(120, 60));
        // Brute-force protection
        r.put("login", new Rule(5, 60));
        // Email spam protection
        r.put("forgot-password", new Rule(3, 60));
        // Account creation spam
        r.put("register", new Rule(5, 3600));
        return r;
    }

    @Getter
    @Setter
    public static class Rule {
        /** Max number of requests allowed in the window. */
        private int capacity;
        /** Window in seconds — capacity refills smoothly over this period. */
        private int windowSeconds;

        public Rule() {}
        public Rule(int capacity, int windowSeconds) {
            this.capacity = capacity;
            this.windowSeconds = windowSeconds;
        }
    }
}
