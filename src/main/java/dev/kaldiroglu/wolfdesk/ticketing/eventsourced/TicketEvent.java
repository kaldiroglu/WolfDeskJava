package dev.kaldiroglu.wolfdesk.ticketing.eventsourced;

import dev.kaldiroglu.wolfdesk.ticketing.EscalationReason;
import dev.kaldiroglu.wolfdesk.ticketing.MessageId;
import dev.kaldiroglu.wolfdesk.ticketing.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.ResponseTimeframe;
import dev.kaldiroglu.wolfdesk.ticketing.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.UserId;

import java.time.Instant;
import java.util.List;

/**
 * The ticket's domain events, its source of truth. Chapter 6's events tell the rest of the
 * system what happened; these must also be enough to rebuild the ticket, so each one carries
 * every value it changes: the title, the response timeframe, the message body.
 */
public sealed interface TicketEvent {
    TicketId ticketId();

    Instant occurredAt();

    /** A customer opened a ticket. */
    record TicketOpened(TicketId ticketId, UserId customer, List<ProductId> products, String title,
                        Priority priority, Instant occurredAt) implements TicketEvent {
    }

    /** An agent was assigned, with the time the agent has to respond. */
    record TicketAssigned(TicketId ticketId, UserId agent, ResponseTimeframe timeframe,
                          Instant occurredAt) implements TicketEvent {
    }

    /** The customer or the agent added a message. */
    record MessageAdded(TicketId ticketId, MessageId messageId, UserId from, UserId to, String body,
                        Instant occurredAt) implements TicketEvent {
    }

    /** The receiver acknowledged a message. */
    record MessageRead(TicketId ticketId, MessageId messageId, Instant occurredAt) implements TicketEvent {
    }

    /** The ticket was escalated: the deadline passed, or the customer asked. */
    record TicketEscalated(TicketId ticketId, EscalationReason reason, Instant occurredAt) implements TicketEvent {
    }

    /** An escalated ticket went to another agent. */
    record TicketReassigned(TicketId ticketId, UserId previousAgent, UserId newAgent,
                            Instant occurredAt) implements TicketEvent {
    }

    /** The customer or the agent closed the ticket. */
    record TicketClosed(TicketId ticketId, UserId closedBy, Instant occurredAt) implements TicketEvent {
    }
}
