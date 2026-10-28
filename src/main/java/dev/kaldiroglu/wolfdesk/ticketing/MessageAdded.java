package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;

/** The customer or the agent added a message. */
public record MessageAdded(TicketId ticketId, MessageId messageId, UserId from, UserId to, Instant occurredAt) implements DomainEvent {
}
