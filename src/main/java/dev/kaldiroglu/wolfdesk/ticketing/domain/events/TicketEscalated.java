package dev.kaldiroglu.wolfdesk.ticketing.domain.events;

import dev.kaldiroglu.wolfdesk.ticketing.domain.EscalationReason;
import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketId;

import java.time.Instant;

/** The ticket was escalated: the deadline passed, or the customer asked (listing 06-16). */
public record TicketEscalated(TicketId ticketId, EscalationReason reason, Instant occurredAt) implements DomainEvent {
}
