package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;

/** The ticket was escalated: the deadline passed, or the customer asked (listing 06-16). */
public record TicketEscalated(TicketId ticketId, EscalationReason reason, Instant occurredAt) implements DomainEvent {
}
