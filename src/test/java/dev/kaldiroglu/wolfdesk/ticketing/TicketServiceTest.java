package dev.kaldiroglu.wolfdesk.ticketing;

import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryDepartmentRepository;
import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryEventPublisher;
import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TicketServiceTest {

    private static final Instant OPENED = Instant.parse("2026-10-28T10:00:00Z");

    private final UserId customer = UserId.random();
    private final UserId agent = UserId.random();
    private InMemoryTicketRepository tickets;
    private InMemoryEventPublisher publisher;
    private ResponseTimeframeCalculationService timeframes;

    @BeforeEach
    void setUp() {
        tickets = new InMemoryTicketRepository();
        publisher = new InMemoryEventPublisher();
        var departments = new InMemoryDepartmentRepository();
        departments.setPolicy(agent, new DepartmentPolicy(Map.of(Priority.HIGH, Duration.ofHours(2)),
                0.5, Duration.ofDays(1)));
        departments.addShift(agent, new Shift(Instant.parse("2026-10-28T09:00:00Z"),
                Instant.parse("2026-10-28T17:00:00Z")));
        timeframes = new ResponseTimeframeCalculationService(departments);
    }

    private TicketService serviceAt(Instant now, TicketRepository repository) {
        return new TicketService(repository, timeframes, publisher, Clock.fixed(now, ZoneOffset.UTC));
    }

    private TicketId openAndAssign() {
        var service = serviceAt(OPENED, tickets);
        var id = service.open(customer, List.of(ProductId.random()), "Cannot log in", Priority.HIGH);
        service.assign(id, agent);
        return id;
    }

    @Test
    void theEventsArePublishedAfterTheTicketIsSaved() {
        openAndAssign();

        var types = publisher.published().stream().map(e -> e.getClass().getSimpleName()).toList();
        assertEquals(List.of("TicketOpened", "TicketAssigned"), types);
    }

    @Test
    void theAgentGetsADeadlineFromTheDomainService() {
        var id = openAndAssign();

        var assigned = (TicketAssigned) publisher.published().get(1);
        assertEquals(id, assigned.ticketId());
        assertEquals(Instant.parse("2026-10-28T12:00:00Z"), assigned.responseDeadline());
    }

    @Test
    void anEscalationAfterTheDeadlineSucceeds() {
        var id = openAndAssign();

        var result = serviceAt(Instant.parse("2026-10-28T12:00:00Z"), tickets).escalate(id, customer);

        assertInstanceOf(ExecutionResult.Success.class, result);
        assertTrue(tickets.load(id).isEscalated());
    }

    @Test
    void aConcurrentChangeIsReportedAsAFailure() {
        var id = openAndAssign();
        // Another request saves the same ticket between this request's load and save.
        var racing = new TicketRepository() {
            @Override
            public Ticket load(TicketId ticketId) {
                var loaded = tickets.load(ticketId);
                tickets.save(tickets.load(ticketId));
                return loaded;
            }

            @Override
            public void save(Ticket ticket) {
                tickets.save(ticket);
            }
        };

        var result = serviceAt(Instant.parse("2026-10-28T12:00:00Z"), racing).escalate(id, customer);

        assertInstanceOf(ExecutionResult.Failure.class, result);
        assertFalse(tickets.load(id).isEscalated());
    }

    @Test
    void aRejectedEscalationIsReportedAsAFailure() {
        var id = openAndAssign();

        var result = serviceAt(OPENED, tickets).escalate(id, customer);

        assertEquals(new ExecutionResult.Failure("The response deadline has not passed yet"), result);
    }
}
