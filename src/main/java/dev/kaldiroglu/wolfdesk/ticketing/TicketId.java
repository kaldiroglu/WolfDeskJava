package dev.kaldiroglu.wolfdesk.ticketing;

import java.util.Objects;
import java.util.UUID;

/** A ticket's identity. A value object: two ids with the same value are equal. */
public record TicketId(UUID value) {

    public TicketId {
        Objects.requireNonNull(value, "value");
    }

    public static TicketId random() {
        return new TicketId(UUID.randomUUID());
    }
}
