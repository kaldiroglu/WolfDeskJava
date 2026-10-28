package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;

/** The receiver acknowledged a message. */
public record MessageRead(TicketId ticketId, MessageId messageId, Instant occurredAt) implements DomainEvent {
}
