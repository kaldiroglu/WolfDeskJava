package dev.kaldiroglu.wolfdesk.ticketing.domain.events;

import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.UserId;

import java.time.Instant;

/** The customer or the agent closed the ticket. */
public record TicketClosed(TicketId ticketId, UserId closedBy, Instant occurredAt) implements DomainEvent {
}
