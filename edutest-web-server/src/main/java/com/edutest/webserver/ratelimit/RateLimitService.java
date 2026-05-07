package com.edutest.webserver.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token-bucket store keyed by {@code <ruleName>:<clientKey>}.
 *
 * Buckets are created on first use and never explicitly evicted — the map size
 * is bounded by (rules × distinct users), which is fine for a single-instance
 * deployment. For multi-instance, swap to a distributed grid (e.g. Bucket4j
 * with Hazelcast/Redis backend).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitProperties properties;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Result of a consume attempt.
     *
     * @param allowed         true if the request was allowed (token consumed)
     * @param retryAfterSeconds when {@code allowed=false}, seconds until the next token is available
     */
    public record ConsumeResult(boolean allowed, long retryAfterSeconds) {}

    public ConsumeResult tryConsume(String ruleName, String clientKey) {
        if (!properties.isEnabled()) {
            return new ConsumeResult(true, 0);
        }
        RateLimitProperties.Rule rule = properties.getRules().get(ruleName);
        if (rule == null) {
            log.warn("No rate-limit rule '{}' configured — allowing", ruleName);
            return new ConsumeResult(true, 0);
        }

        String mapKey = ruleName + ":" + clientKey;
        Bucket bucket = buckets.computeIfAbsent(mapKey, k -> newBucket(rule));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return new ConsumeResult(true, 0);
        }
        long waitSec = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        return new ConsumeResult(false, waitSec);
    }

    private static Bucket newBucket(RateLimitProperties.Rule rule) {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(rule.getCapacity())
                .refillGreedy(rule.getCapacity(), Duration.ofSeconds(rule.getWindowSeconds()))
                .build();
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
