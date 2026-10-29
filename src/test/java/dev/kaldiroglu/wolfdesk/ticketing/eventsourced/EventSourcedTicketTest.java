package dev.kaldiroglu.wolfdesk.ticketing.eventsourced;

import dev.kaldiroglu.wolfdesk.ticketing.AddMessage;
import dev.kaldiroglu.wolfdesk.ticketing.AssignAgent;
import dev.kaldiroglu.wolfdesk.ticketing.CloseTicket;
import dev.kaldiroglu.wolfdesk.ticketing.EvaluateAutomaticActions;
import dev.kaldiroglu.wolfdesk.ticketing.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.RequestEscalation;
import dev.kaldiroglu.wolfdesk.ticketing.ResponseTimeframe;
import dev.kaldiroglu.wolfdesk.ticketing.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.TicketRuleViolationException;
import dev.kaldiroglu.wolfdesk.ticketing.TicketStatus;
import dev.kaldiroglu.wolfdesk.ticketing.UserId;
import dev.kaldiroglu.wolfdesk.ticketing.eventsourced.TicketEvent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EventSourcedTicketTest {

    private static final Instant OPENED = Instant.parse("2026-10-28T20:00:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-10-28T22:00:00Z");

    private final UserId customer = UserId.random();
    private final UserId agent = UserId.random();
    private final UserId otherAgent = UserId.random();
    private Ticket ticket;

    @BeforeEach
    void openAndAssign() {
        ticket = Ticket.open(TicketId.random(), customer, List.of(ProductId.random()),
                "Cannot log in", Priority.HIGH, OPENED);
        ticket.execute(new AssignAgent(agent, new ResponseTimeframe(OPENED, DEADLINE), OPENED));
    }

    private static List<String> typesOf(List<TicketEvent> events) {
        return events.stream().map(e -> e.getClass().getSimpleName()).toList();
    }

    @Test
    void aCommandAppendsAnEventInsteadOfChangingAField() {
        ticket.execute(new AddMessage(customer, "Still broken", OPENED));

        assertEquals(List.of("TicketOpened", "TicketAssigned", "MessageAdded"), typesOf(ticket.newEvents()));
        var added = (MessageAdded) ticket.newEvents().getLast();
        assertEquals("Still broken", added.body());
        assertEquals(agent, added.to());
    }

    @Test
    void aTicketIsRebuiltFromItsEvents() {
        ticket.execute(new AddMessage(customer, "Still broken", OPENED));
        ticket.execute(new RequestEscalation(customer, DEADLINE));

        var rebuilt = new Ticket(ticket.newEvents());

        assertEquals(ticket.id(), rebuilt.id());
        assertEquals("Cannot log in", rebuilt.title());
        assertEquals(Optional.of(agent), rebuilt.assignedAgent());
        assertTrue(rebuilt.isEscalated());
        assertEquals("Still broken", rebuilt.messages().getFirst().body());
        assertEquals(1, rebuilt.getUnreadMessagesCount(agent));
        assertTrue(rebuilt.newEvents().isEmpty());
    }

    @Test
    void theVersionIsTheNumberOfTheLastEvent() {
        assertEquals(-1, new Ticket(List.of()).version());
        assertEquals(0, new Ticket(ticket.newEvents().subList(0, 1)).version());
        assertEquals(1, ticket.version());
    }

    @Test
    void anEarlierStateIsProjectedFromTheFirstEvents() {
        ticket.execute(new AddMessage(customer, "Still broken", OPENED));

        var asOpened = new Ticket(ticket.newEvents().subList(0, 1));

        assertEquals(Optional.empty(), asOpened.assignedAgent());
        assertTrue(asOpened.messages().isEmpty());
        assertEquals(1, ticket.messages().size());
    }

    @Test
    void aRejectedCommandAppendsNoEvent() {
        var before = ticket.newEvents();

        assertThrows(TicketRuleViolationException.class,
                () -> ticket.execute(new RequestEscalation(customer, OPENED)));
        assertEquals(before, ticket.newEvents());
    }

    @Test
    void aRebuiltTicketKeepsItsRules() {
        ticket.execute(new RequestEscalation(customer, DEADLINE));
        var rebuilt = new Ticket(ticket.newEvents());

        var ex = assertThrows(TicketRuleViolationException.class,
                () -> rebuilt.execute(new RequestEscalation(customer, DEADLINE)));
        assertEquals("The ticket is already escalated", ex.getMessage());
    }

    @Test
    void anEscalatedTicketWithUnreadMessagesIsReassigned() {
        ticket.execute(new AddMessage(customer, "Still broken", OPENED));

        ticket.execute(new EvaluateAutomaticActions(DEADLINE, otherAgent));

        var events = ticket.newEvents();
        assertEquals(List.of("TicketEscalated", "TicketReassigned"), typesOf(events.subList(3, 5)));
        assertEquals(Optional.of(otherAgent), ticket.assignedAgent());
    }

    @Test
    void aClosedTicketTakesNoMoreCommands() {
        ticket.execute(new CloseTicket(customer, OPENED));

        assertEquals(TicketStatus.CLOSED, ticket.status());
        assertThrows(TicketRuleViolationException.class,
                () -> ticket.execute(new AddMessage(customer, "One more thing", OPENED)));
    }
}
