package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Duration;
import java.util.Map;

/**
 * A support department's response rules: the working time an agent has per priority, the
 * factor applied to it when a ticket is escalated, and how far ahead to look for shifts.
 */
public record DepartmentPolicy(Map<Priority, Duration> maxResponseTimes, double escalationFactor,
                               Duration maxAgentResponseTime) {

    public DepartmentPolicy {
        maxResponseTimes = Map.copyOf(maxResponseTimes);
    }

    public Duration getMaxResponseTimeFor(Priority priority) {
        var time = maxResponseTimes.get(priority);
        if (time == null) throw new IllegalArgumentException("No response time for " + priority);
        return time;
    }
}
