package dev.kaldiroglu.wolfdesk.ticketing.inmemory;

import dev.kaldiroglu.wolfdesk.ticketing.DepartmentPolicy;
import dev.kaldiroglu.wolfdesk.ticketing.DepartmentRepository;
import dev.kaldiroglu.wolfdesk.ticketing.Shift;
import dev.kaldiroglu.wolfdesk.ticketing.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Policies and shift schedules held in maps. */
public class InMemoryDepartmentRepository implements DepartmentRepository {

    private final Map<UserId, DepartmentPolicy> policies = new HashMap<>();
    private final Map<UserId, List<Shift>> shifts = new HashMap<>();

    public void setPolicy(UserId agentId, DepartmentPolicy policy) {
        policies.put(agentId, policy);
    }

    public void addShift(UserId agentId, Shift shift) {
        shifts.computeIfAbsent(agentId, k -> new ArrayList<>()).add(shift);
    }

    @Override
    public DepartmentPolicy getDepartmentPolicy(UserId agentId) {
        var policy = policies.get(agentId);
        if (policy == null) throw new IllegalArgumentException("No department for agent " + agentId);
        return policy;
    }

    @Override
    public List<Shift> getUpcomingShifts(UserId agentId, Instant from, Instant to) {
        return shifts.getOrDefault(agentId, List.of()).stream()
                .filter(s -> s.end().isAfter(from) && s.start().isBefore(to))
                .toList();
    }
}
