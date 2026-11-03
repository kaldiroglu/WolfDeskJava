package dev.kaldiroglu.wolfdesk.ticketing.domain.events;

import dev.kaldiroglu.wolfdesk.ticketing.domain.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.domain.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.UserId;

import java.time.Instant;
import java.util.List;

/** A customer opened a ticket. */
public record TicketOpened(TicketId ticketId, UserId customer, List<ProductId> products, Priority priority,
                           Instant occurredAt) implements DomainEvent {
}
