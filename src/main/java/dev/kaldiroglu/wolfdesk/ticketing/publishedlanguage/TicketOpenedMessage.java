package dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A ticket was opened for the given products. */
public record TicketOpenedMessage(UUID ticketId, List<UUID> products, String priority, Instant occurredAt)
        implements TicketingMessage {

    public TicketOpenedMessage {
        products = List.copyOf(products);
    }
}
