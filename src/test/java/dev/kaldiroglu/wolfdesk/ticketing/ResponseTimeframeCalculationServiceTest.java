package dev.kaldiroglu.wolfdesk.ticketing;

import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryDepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTimeframeCalculationServiceTest {

    private final UserId agent = UserId.random();
    private InMemoryDepartmentRepository departments;
    private ResponseTimeframeCalculationService service;

    @BeforeEach
    void setUp() {
        departments = new InMemoryDepartmentRepository();
        departments.setPolicy(agent, new DepartmentPolicy(
                Map.of(Priority.HIGH, Duration.ofHours(2), Priority.LOW, Duration.ofHours(8)),
                0.5, Duration.ofDays(3)));
        departments.addShift(agent, new Shift(Instant.parse("2026-10-28T09:00:00Z"),
                Instant.parse("2026-10-28T17:00:00Z")));
        departments.addShift(agent, new Shift(Instant.parse("2026-10-29T09:00:00Z"),
                Instant.parse("2026-10-29T17:00:00Z")));
        service = new ResponseTimeframeCalculationService(departments);
    }

    @Test
    void theDeadlineFallsInsideTheCurrentShift() {
        var start = Instant.parse("2026-10-28T10:00:00Z");

        var timeframe = service.calculateAgentResponseDeadline(agent, Priority.HIGH, false, start);

        assertEquals(Instant.parse("2026-10-28T12:00:00Z"), timeframe.deadline());
    }

    @Test
    void timeOutsideTheShiftsDoesNotCount() {
        var start = Instant.parse("2026-10-28T16:00:00Z");   // one working hour left today

        var timeframe = service.calculateAgentResponseDeadline(agent, Priority.HIGH, false, start);

        assertEquals(Instant.parse("2026-10-29T10:00:00Z"), timeframe.deadline());
    }

    @Test
    void anEscalatedTicketGetsTheEscalationFactor() {
        var start = Instant.parse("2026-10-28T10:00:00Z");

        var timeframe = service.calculateAgentResponseDeadline(agent, Priority.HIGH, true, start);

        assertEquals(Instant.parse("2026-10-28T11:00:00Z"), timeframe.deadline());
    }

    @Test
    void notEnoughShiftsIsAnError() {
        var start = Instant.parse("2026-10-29T16:00:00Z");

        assertThrows(IllegalStateException.class,
                () -> service.calculateAgentResponseDeadline(agent, Priority.LOW, false, start));
    }
}
