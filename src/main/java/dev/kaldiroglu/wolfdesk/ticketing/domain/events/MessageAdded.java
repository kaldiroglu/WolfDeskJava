package dev.kaldiroglu.wolfdesk.ticketing.domain.events;

import dev.kaldiroglu.wolfdesk.ticketing.domain.MessageId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.UserId;

import java.time.Instant;

/** The customer or the agent added a message. */
public record MessageAdded(TicketId ticketId, MessageId messageId, UserId from, UserId to, Instant occurredAt) implements DomainEvent {
}
