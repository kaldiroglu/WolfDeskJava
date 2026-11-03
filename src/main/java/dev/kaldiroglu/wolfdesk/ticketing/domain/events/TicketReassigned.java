package dev.kaldiroglu.wolfdesk.ticketing.domain.events;

import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.UserId;

import java.time.Instant;

/** An escalated ticket went to another agent. */
public record TicketReassigned(TicketId ticketId, UserId previousAgent, UserId newAgent, Instant occurredAt) implements DomainEvent {
}
