package dev.kaldiroglu.wolfdesk.ticketing;

import java.util.Objects;
import java.util.UUID;

/** A product of the tenant, referenced by its id only. A value object: two ids with the same value are equal. */
public record ProductId(UUID value) {

    public ProductId {
        Objects.requireNonNull(value, "value");
    }

    public static ProductId random() {
        return new ProductId(UUID.randomUUID());
    }
}
