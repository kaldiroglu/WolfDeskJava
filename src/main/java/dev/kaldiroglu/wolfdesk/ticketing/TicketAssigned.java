package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;

/** An agent was assigned, with a deadline to respond. */
public record TicketAssigned(TicketId ticketId, UserId agent, Instant responseDeadline, Instant occurredAt) implements DomainEvent {
}
