package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * The time an agent has to respond: from {@code start} to {@code deadline}. A value object
 * that validates itself and computes how much of the time is left.
 */
public record ResponseTimeframe(Instant start, Instant deadline) {

    public ResponseTimeframe {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(deadline, "deadline");
        if (!deadline.isAfter(start)) {
            throw new IllegalArgumentException("The deadline must come after the start");
        }
    }

    /** 1.0 at the start, 0.0 at the deadline, negative once the deadline has passed. */
    public double remainingTimePercentage(Instant now) {
        var total = Duration.between(start, deadline).toMillis();
        var left = Duration.between(now, deadline).toMillis();
        return (double) left / total;
    }
}
