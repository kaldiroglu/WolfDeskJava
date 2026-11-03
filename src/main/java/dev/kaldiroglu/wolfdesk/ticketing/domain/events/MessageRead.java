package dev.kaldiroglu.wolfdesk.ticketing.domain.events;

import dev.kaldiroglu.wolfdesk.ticketing.domain.MessageId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketId;

import java.time.Instant;

/** The receiver acknowledged a message. */
public record MessageRead(TicketId ticketId, MessageId messageId, Instant occurredAt) implements DomainEvent {
}
