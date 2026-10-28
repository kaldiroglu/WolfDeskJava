package dev.kaldiroglu.wolfdesk.ticketing.readmodels;

import dev.kaldiroglu.wolfdesk.ticketing.DepartmentPolicy;
import dev.kaldiroglu.wolfdesk.ticketing.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.ResponseTimeframeCalculationService;
import dev.kaldiroglu.wolfdesk.ticketing.Shift;
import dev.kaldiroglu.wolfdesk.ticketing.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.TicketService;
import dev.kaldiroglu.wolfdesk.ticketing.UserId;
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

class AgentQueueProjectionTest {

    private static final Instant OPENED = Instant.parse("2026-10-28T10:00:00Z");

    private final UserId customer = UserId.random();
    private final UserId agent = UserId.random();
    private final UserId otherAgent = UserId.random();
    private InMemoryTicketRepository tickets;
    private ResponseTimeframeCalculationService timeframes;
    private AgentQueueProjection projection;

    @BeforeEach
    void setUp() {
        tickets = new InMemoryTicketRepository();
        var departments = new InMemoryDepartmentRepository();
        departments.setPolicy(agent, new DepartmentPolicy(
                Map.of(Priority.HIGH, Duration.ofHours(2), Priority.LOW, Duration.ofHours(4)),
                0.5, Duration.ofDays(1)));
        departments.addShift(agent, new Shift(Instant.parse("2026-10-28T09:00:00Z"),
                Instant.parse("2026-10-28T17:00:00Z")));
        timeframes = new ResponseTimeframeCalculationService(departments);
        projection = new AgentQueueProjection(tickets);
    }

    private TicketService serviceAt(Instant now) {
        return new TicketService(tickets, timeframes, new InMemoryEventPublisher(),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private TicketId openAndAssign(String title, Priority priority) {
        var service = serviceAt(OPENED);
        var id = service.open(customer, List.of(ProductId.random()), title, priority);
        service.assign(id, agent);
        return id;
    }

    private List<TicketId> queueOf(UserId user) {
        return projection.openTicketsOf(user).stream().map(QueuedTicket::id).toList();
    }

    @Test
    void anAssignedTicketAppearsInItsAgentsQueue() {
        var id = openAndAssign("Cannot log in", Priority.HIGH);

        projection.catchUp();

        var queued = projection.openTicketsOf(agent).getFirst();
        assertEquals(new QueuedTicket(id, "Cannot log in", Priority.HIGH,
                Instant.parse("2026-10-28T12:00:00Z"), false), queued);
    }

    @Test
    void theQueueListsTheEarliestDeadlineFirst() {
        var low = openAndAssign("Typo on the invoice", Priority.LOW);
        var high = openAndAssign("Cannot log in", Priority.HIGH);

        projection.catchUp();

        assertEquals(List.of(high, low), queueOf(agent));
    }

    @Test
    void theQueueChangesOnlyWhenTheProjectionCatchesUp() {
        var id = openAndAssign("Cannot log in", Priority.HIGH);
        projection.catchUp();

        serviceAt(OPENED).close(id, customer);

        assertEquals(List.of(id), queueOf(agent));
        projection.catchUp();
        assertEquals(List.of(), queueOf(agent));
    }

    @Test
    void aCatchUpReadsOnlyTheChangesAfterItsCheckpoint() {
        openAndAssign("Cannot log in", Priority.HIGH);
        openAndAssign("Typo on the invoice", Priority.LOW);

        assertEquals(2, projection.catchUp());
        assertEquals(4, projection.checkpoint());
        assertEquals(0, projection.catchUp());
    }

    @Test
    void aClosedTicketLeavesTheQueue() {
        var closed = openAndAssign("Cannot log in", Priority.HIGH);
        var open = openAndAssign("Typo on the invoice", Priority.LOW);
        serviceAt(OPENED).close(closed, agent);

        projection.catchUp();

        assertEquals(List.of(open), queueOf(agent));
    }

    @Test
    void aReassignedTicketMovesToTheNewAgentsQueue() {
        var id = openAndAssign("Cannot log in", Priority.HIGH);
        serviceAt(OPENED).addMessage(id, customer, "Still locked out");
        projection.catchUp();

        serviceAt(Instant.parse("2026-10-28T12:00:00Z")).evaluateAutomaticActions(id, otherAgent);
        projection.catchUp();

        assertEquals(List.of(), queueOf(agent));
        assertEquals(List.of(id), queueOf(otherAgent));
        assertTrue(projection.openTicketsOf(otherAgent).getFirst().escalated());
    }

    @Test
    void aResetProjectionRebuildsTheSameQueues() {
        openAndAssign("Cannot log in", Priority.HIGH);
        openAndAssign("Typo on the invoice", Priority.LOW);
        projection.catchUp();
        var before = projection.openTicketsOf(agent);

        projection.reset();
        assertEquals(List.of(), projection.openTicketsOf(agent));
        projection.catchUp();

        assertEquals(before, projection.openTicketsOf(agent));
    }

    @Test
    void aProjectionAddedLaterSeesEveryTicket() {
        var first = openAndAssign("Cannot log in", Priority.HIGH);
        projection.catchUp();
        var second = openAndAssign("Typo on the invoice", Priority.LOW);

        var later = new AgentQueueProjection(tickets);
        later.catchUp();

        assertEquals(List.of(first, second),
                later.openTicketsOf(agent).stream().map(QueuedTicket::id).toList());
    }
}
