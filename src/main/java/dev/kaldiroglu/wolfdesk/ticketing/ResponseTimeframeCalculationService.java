package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * A domain service (listing 06-18): stateless business logic that belongs to no single
 * aggregate. It reads the department's policy and the agent's shifts, and returns the time
 * the agent has to respond, counted in working time only.
 */
public final class ResponseTimeframeCalculationService {

    private final DepartmentRepository departmentRepository;

    public ResponseTimeframeCalculationService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public ResponseTimeframe calculateAgentResponseDeadline(UserId agentId, Priority priority,
                                                            boolean escalated, Instant startTime) {
        var policy = departmentRepository.getDepartmentPolicy(agentId);
        var maxProcTime = policy.getMaxResponseTimeFor(priority);

        if (escalated) {
            maxProcTime = Duration.ofMillis(Math.round(maxProcTime.toMillis() * policy.escalationFactor()));
        }

        var shifts = departmentRepository.getUpcomingShifts(agentId,
                startTime, startTime.plus(policy.maxAgentResponseTime()));

        return calculateTargetTime(startTime, maxProcTime, shifts);
    }

    /** Spends the working time shift by shift, from the start time on. */
    private static ResponseTimeframe calculateTargetTime(Instant startTime, Duration workingTime,
                                                         List<Shift> shifts) {
        var left = workingTime;
        for (var shift : shifts.stream().sorted(Comparator.comparing(Shift::start)).toList()) {
            var from = shift.start().isAfter(startTime) ? shift.start() : startTime;
            if (!from.isBefore(shift.end())) continue;
            var available = Duration.between(from, shift.end());
            if (available.compareTo(left) >= 0) {
                return new ResponseTimeframe(startTime, from.plus(left));
            }
            left = left.minus(available);
        }
        throw new IllegalStateException("The agent's shifts do not cover the response time");
    }
}
