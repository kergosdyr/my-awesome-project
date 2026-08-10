package io.github.kergosdyr.commercelab.domain.cache;

import java.time.Duration;

public record CacheExperimentPolicy(
        Duration ttl,
        Duration originDelay,
        Duration lockLease,
        Duration lockWaitTimeout,
        Duration lockPollInterval
) {

    public CacheExperimentPolicy {
        requirePositive(ttl, "ttl");
        requireNonNegative(originDelay, "originDelay");
        requirePositive(lockLease, "lockLease");
        requirePositive(lockWaitTimeout, "lockWaitTimeout");
        requirePositive(lockPollInterval, "lockPollInterval");

        if (lockLease.compareTo(originDelay) <= 0) {
            throw new IllegalArgumentException("lockLease must be longer than originDelay");
        }
        if (lockWaitTimeout.compareTo(lockPollInterval) < 0) {
            throw new IllegalArgumentException("lockWaitTimeout must not be shorter than lockPollInterval");
        }
    }

    public int lockAttemptCount() {
        var waitNanos = lockWaitTimeout.toNanos();
        var pollNanos = lockPollInterval.toNanos();
        var attempts = waitNanos / pollNanos;
        return Math.toIntExact(attempts);
    }

    private static void requirePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireNonNegative(Duration duration, String name) {
        if (duration == null || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
