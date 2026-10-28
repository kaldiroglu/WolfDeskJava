package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;
import java.util.List;

public interface DepartmentRepository {

    DepartmentPolicy getDepartmentPolicy(UserId agentId);

    /** The agent's shifts that overlap the period from {@code from} to {@code to}. */
    List<Shift> getUpcomingShifts(UserId agentId, Instant from, Instant to);
}
