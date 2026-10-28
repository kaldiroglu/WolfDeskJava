package dev.kaldiroglu.wolfdesk.ticketing;

import java.util.Objects;
import java.util.UUID;

/** A customer or a support agent. A value object: two ids with the same value are equal. */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "value");
    }

    public static UserId random() {
        return new UserId(UUID.randomUUID());
    }
}
