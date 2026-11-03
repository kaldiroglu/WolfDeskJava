package dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage;

import java.time.Instant;
import java.util.UUID;

/** A ticket was escalated. */
public record TicketEscalatedMessage(UUID ticketId, Instant occurredAt) implements TicketingMessage {
}
