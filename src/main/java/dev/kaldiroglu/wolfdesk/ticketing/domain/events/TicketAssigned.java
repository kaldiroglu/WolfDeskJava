package dev.kaldiroglu.wolfdesk.ticketing.domain.events;

import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.UserId;

import java.time.Instant;

/** An agent was assigned, with a deadline to respond. */
public record TicketAssigned(TicketId ticketId, UserId agent, Instant responseDeadline, Instant occurredAt) implements DomainEvent {
}
