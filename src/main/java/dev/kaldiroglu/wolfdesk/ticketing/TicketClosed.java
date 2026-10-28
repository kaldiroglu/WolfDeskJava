package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;

/** The customer or the agent closed the ticket. */
public record TicketClosed(TicketId ticketId, UserId closedBy, Instant occurredAt) implements DomainEvent {
}
