package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;
import java.util.List;

/**
 * A ticket's state as plain data, for the repository to store and load. The domain model
 * itself never depends on how it is stored.
 */
public record TicketSnapshot(TicketId id, int version, UserId customer, List<ProductId> products,
                             String title, Priority priority, UserId assignedAgent,
                             ResponseTimeframe responseTimeframe, boolean escalated,
                             TicketStatus status, List<MessageSnapshot> messages) {

    public record MessageSnapshot(MessageId id, UserId from, UserId to, String body,
                                  Instant sentAt, boolean wasRead) {
    }

    public TicketSnapshot {
        products = List.copyOf(products);
        messages = List.copyOf(messages);
    }

    public TicketSnapshot withVersion(int newVersion) {
        return new TicketSnapshot(id, newVersion, customer, products, title, priority,
                assignedAgent, responseTimeframe, escalated, status, messages);
    }
}
