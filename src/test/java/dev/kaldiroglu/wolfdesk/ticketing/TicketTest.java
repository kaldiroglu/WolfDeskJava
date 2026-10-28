package dev.kaldiroglu.wolfdesk.ticketing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TicketTest {

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
        ticket.pullDomainEvents();
    }

    @Test
    void openingATicketRecordsTicketOpened() {
        var t = Ticket.open(TicketId.random(), customer, List.of(ProductId.random()),
                "Cannot log in", Priority.LOW, OPENED);
        assertInstanceOf(TicketOpened.class, t.pullDomainEvents().getFirst());
    }

    @Test
    void aTicketIsAboutAtLeastOneProduct() {
        assertThrows(TicketRuleViolationException.class, () ->
                Ticket.open(TicketId.random(), customer, List.of(), "Cannot log in", Priority.LOW, OPENED));
    }

    @Test
    void aMessageFromTheCustomerGoesToTheAgent() {
        ticket.execute(new AddMessage(customer, "Still broken", OPENED));

        assertEquals(agent, ticket.messages().getFirst().to());
        assertEquals(1, ticket.getUnreadMessagesCount(agent));
    }

    @Test
    void onlyTheCustomerOrTheAgentCanWrite() {
        assertThrows(TicketRuleViolationException.class,
                () -> ticket.execute(new AddMessage(otherAgent, "Hello", OPENED)));
    }

    @Test
    void onlyTheReceiverCanAcknowledgeAMessage() {
        ticket.execute(new AddMessage(customer, "Still broken", OPENED));
        var id = ticket.messages().getFirst().id();

        assertThrows(TicketRuleViolationException.class,
                () -> ticket.execute(new AcknowledgeMessage(id, customer, OPENED)));
        ticket.execute(new AcknowledgeMessage(id, agent, OPENED));
        assertEquals(0, ticket.getUnreadMessagesCount(agent));
    }

    @Test
    void anEscalationCannotBeRequestedBeforeTheDeadline() {
        assertThrows(TicketRuleViolationException.class,
                () -> ticket.execute(new RequestEscalation(customer, DEADLINE.minusSeconds(1))));
        assertFalse(ticket.isEscalated());
    }

    @Test
    void anEscalationRequestedAfterTheDeadlineEscalatesTheTicket() {
        ticket.execute(new RequestEscalation(customer, DEADLINE));

        assertTrue(ticket.isEscalated());
        var event = assertInstanceOf(TicketEscalated.class, ticket.pullDomainEvents().getFirst());
        assertEquals(EscalationReason.CUSTOMER_REQUEST, event.reason());
    }

    @Test
    void aTicketIsEscalatedOnlyOnce() {
        ticket.execute(new RequestEscalation(customer, DEADLINE));
        assertThrows(TicketRuleViolationException.class,
                () -> ticket.execute(new RequestEscalation(customer, DEADLINE)));
    }

    @Test
    void onlyTheCustomerCanRequestAnEscalation() {
        assertThrows(TicketRuleViolationException.class,
                () -> ticket.execute(new RequestEscalation(agent, DEADLINE)));
    }

    @Test
    void aMissedDeadlineEscalatesTheTicketAutomatically() {
        ticket.execute(new EvaluateAutomaticActions(DEADLINE, otherAgent));

        assertTrue(ticket.isEscalated());
        var event = assertInstanceOf(TicketEscalated.class, ticket.pullDomainEvents().getFirst());
        assertEquals(EscalationReason.MISSED_SLA, event.reason());
    }

    @Test
    void anEscalatedTicketWithUnreadMessagesIsReassigned() {
        ticket.execute(new AddMessage(customer, "Anyone there?", OPENED));
        ticket.execute(new RequestEscalation(customer, DEADLINE));

        ticket.execute(new EvaluateAutomaticActions(DEADLINE, otherAgent));

        assertEquals(otherAgent, ticket.assignedAgent().orElseThrow());
        assertTrue(ticket.pullDomainEvents().stream().anyMatch(TicketReassigned.class::isInstance));
    }

    @Test
    void anEscalatedTicketWhoseMessagesWereReadKeepsItsAgent() {
        ticket.execute(new AddMessage(customer, "Anyone there?", OPENED));
        ticket.execute(new AcknowledgeMessage(ticket.messages().getFirst().id(), agent, OPENED));
        ticket.execute(new RequestEscalation(customer, DEADLINE));

        ticket.execute(new EvaluateAutomaticActions(DEADLINE, otherAgent));

        assertEquals(agent, ticket.assignedAgent().orElseThrow());
    }

    @Test
    void anEscalatedTicketWithMoreThanHalfItsTimeLeftKeepsItsAgent() {
        ticket.execute(new AddMessage(customer, "Anyone there?", OPENED));
        ticket.execute(new RequestEscalation(customer, DEADLINE));
        ticket.execute(new AssignAgent(agent, new ResponseTimeframe(DEADLINE, DEADLINE.plusSeconds(7200)), DEADLINE));

        ticket.execute(new EvaluateAutomaticActions(DEADLINE.plusSeconds(3000), otherAgent));

        assertEquals(agent, ticket.assignedAgent().orElseThrow());
    }

    @Test
    void aClosedTicketTakesNoMoreMessages() {
        ticket.execute(new CloseTicket(customer, OPENED));

        assertEquals(TicketStatus.CLOSED, ticket.status());
        assertThrows(TicketRuleViolationException.class,
                () -> ticket.execute(new AddMessage(customer, "One more thing", OPENED)));
    }

    @Test
    void aRestoredTicketHasTheSameState() {
        ticket.execute(new AddMessage(customer, "Still broken", OPENED));

        var restored = Ticket.restore(ticket.snapshot());

        assertEquals(ticket.snapshot(), restored.snapshot());
    }
}
