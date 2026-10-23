package io.orangebeard.client;

import java.time.Duration;

/**
 * Retry settings for the synchronous client: {@code maxAttempts} tries in total, waiting
 * {@code initialDelay} before the second one and multiplying the wait by {@code multiplier} each time.
 */
record RetryPolicy(int maxAttempts, Duration initialDelay, double multiplier) {
    static final RetryPolicy DEFAULT = new RetryPolicy(4, Duration.ofSeconds(2), 2.0);

    Duration delayBefore(int nextAttempt) {
        double factor = Math.pow(multiplier, Math.max(0, nextAttempt - 2));
        return Duration.ofMillis((long) (initialDelay.toMillis() * factor));
    }
}
