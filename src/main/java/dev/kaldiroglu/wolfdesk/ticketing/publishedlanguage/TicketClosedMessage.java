package dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage;

import java.time.Instant;
import java.util.UUID;

/** A ticket was closed. */
public record TicketClosedMessage(UUID ticketId, Instant occurredAt) implements TicketingMessage {
}
