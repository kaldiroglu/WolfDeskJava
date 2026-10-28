package dev.kaldiroglu.wolfdesk.billing;

import java.util.Objects;
import java.util.UUID;

/** A tenant, as billing knows it. */
public record TenantId(UUID value) {

    public TenantId {
        Objects.requireNonNull(value, "value");
    }

    public static TenantId random() {
        return new TenantId(UUID.randomUUID());
    }
}
