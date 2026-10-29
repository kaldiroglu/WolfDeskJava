package dev.kaldiroglu.wolfdesk.ticketing.eventsourced;

import dev.kaldiroglu.wolfdesk.ticketing.DepartmentPolicy;
import dev.kaldiroglu.wolfdesk.ticketing.ExecutionResult;
import dev.kaldiroglu.wolfdesk.ticketing.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.ResponseTimeframeCalculationService;
import dev.kaldiroglu.wolfdesk.ticketing.Shift;
import dev.kaldiroglu.wolfdesk.ticketing.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.TicketStatus;
import dev.kaldiroglu.wolfdesk.ticketing.UserId;
import dev.kaldiroglu.wolfdesk.ticketing.eventsourced.TicketEvent.*;
import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryDepartmentRepository;
import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EventSourcedTicketServiceTest {

    private static final Instant OPENED = Instant.parse("2026-10-28T10:00:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-10-28T12:00:00Z");

    private final UserId customer = UserId.random();
    private final UserId agent = UserId.random();
    private final UserId otherAgent = UserId.random();
    private InMemoryEventStore store;
    private ResponseTimeframeCalculationService timeframes;

    @BeforeEach
    void setUp() {
        store = new InMemoryEventStore();
        var departments = new InMemoryDepartmentRepository();
        for (var a : List.of(agent, otherAgent)) {
            departments.setPolicy(a, new DepartmentPolicy(Map.of(Priority.HIGH, Duration.ofHours(2)),
                    0.5, Duration.ofDays(1)));
            departments.addShift(a, new Shift(Instant.parse("2026-10-28T09:00:00Z"),
                    Instant.parse("2026-10-28T17:00:00Z")));
        }
        timeframes = new ResponseTimeframeCalculationService(departments);
    }

    private TicketService serviceAt(Instant now, EventStore eventStore) {
        return new TicketService(eventStore, timeframes, Clock.fixed(now, ZoneOffset.UTC));
    }

    private TicketId openAndAssign() {
        var service = serviceAt(OPENED, store);
        var id = service.open(customer, List.of(ProductId.random()), "Cannot log in", Priority.HIGH);
        service.assign(id, agent);
        return id;
    }

    @Test
    void everyChangeIsKeptInTheEventStore() {
        var id = openAndAssign();
        serviceAt(OPENED, store).addMessage(id, customer, "Still broken");

        var types = store.fetch(id).stream().map(e -> e.getClass().getSimpleName()).toList();
        assertEquals(List.of("TicketOpened", "TicketAssigned", "MessageAdded"), types);
    }

    @Test
    void theAgentsDeadlineIsInTheAssignedEvent() {
        var id = openAndAssign();

        var assigned = (TicketAssigned) store.fetch(id).get(1);
        assertEquals(DEADLINE, assigned.timeframe().deadline());
    }

    @Test
    void aConcurrentAppendIsReportedAsAFailure() {
        var id = openAndAssign();
        EventStore concurrent = new EventStore() {
            @Override
            public List<TicketEvent> fetch(TicketId ticketId) {
                var events = store.fetch(ticketId);
                store.append(ticketId, List.of(new TicketClosed(ticketId, agent, DEADLINE)), events.size() - 1);
                return events;
            }

            @Override
            public void append(TicketId ticketId, List<TicketEvent> newEvents, int expectedVersion) {
                store.append(ticketId, newEvents, expectedVersion);
            }
        };

        var result = serviceAt(DEADLINE, concurrent).escalate(id, customer);

        assertInstanceOf(ExecutionResult.Failure.class, result);
        assertEquals("TicketClosed", store.fetch(id).getLast().getClass().getSimpleName());
    }

    @Test
    void theTicketAtAnEarlierVersionIsRebuiltFromItsFirstEvents() {
        var id = openAndAssign();
        serviceAt(OPENED, store).close(id, customer);
        var service = serviceAt(OPENED, store);

        assertEquals(Optional.empty(), service.loadAt(id, 0).assignedAgent());
        assertEquals(TicketStatus.OPEN, service.loadAt(id, 1).status());
        assertEquals(TicketStatus.CLOSED, service.load(id).status());
    }

    @Test
    void aNewProjectionReadsTheWholeHistory() {
        var id = openAndAssign();
        serviceAt(OPENED, store).addMessage(id, customer, "Still broken");
        serviceAt(DEADLINE, store).evaluateAutomaticActions(id, otherAgent);
        serviceAt(DEADLINE, store).close(id, customer);

        var analysis = TicketAnalysisProjection.of(store.fetch(id));

        assertEquals(1, analysis.messages());
        assertEquals(1, analysis.reassignments());
        assertTrue(analysis.wasEscalated());
        assertEquals(TicketStatus.CLOSED, analysis.status());
        assertEquals(serviceAt(OPENED, store).load(id).version(), analysis.version());
    }
}
