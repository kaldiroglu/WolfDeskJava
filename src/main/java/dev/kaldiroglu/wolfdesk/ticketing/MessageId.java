package dev.kaldiroglu.wolfdesk.ticketing;

import java.util.Objects;
import java.util.UUID;

/** A message's identity inside its ticket. A value object: two ids with the same value are equal. */
public record MessageId(UUID value) {

    public MessageId {
        Objects.requireNonNull(value, "value");
    }

    public static MessageId random() {
        return new MessageId(UUID.randomUUID());
    }
}
