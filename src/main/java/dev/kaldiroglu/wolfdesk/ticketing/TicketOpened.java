package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;
import java.util.List;

/** A customer opened a ticket. */
public record TicketOpened(TicketId ticketId, UserId customer, List<ProductId> products, Priority priority,
                           Instant occurredAt) implements DomainEvent {
}
