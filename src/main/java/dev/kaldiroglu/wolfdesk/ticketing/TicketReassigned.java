package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;

/** An escalated ticket went to another agent. */
public record TicketReassigned(TicketId ticketId, UserId previousAgent, UserId newAgent, Instant occurredAt) implements DomainEvent {
}
