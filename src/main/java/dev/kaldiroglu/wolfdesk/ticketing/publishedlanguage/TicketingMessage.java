package dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage;

import java.time.Instant;
import java.util.UUID;

/** A public message of ticketing's published language. */
public sealed interface TicketingMessage permits TicketOpenedMessage, TicketEscalatedMessage, TicketClosedMessage {

    UUID ticketId();

    Instant occurredAt();
}
