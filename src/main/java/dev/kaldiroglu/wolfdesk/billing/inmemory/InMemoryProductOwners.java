package dev.kaldiroglu.wolfdesk.billing.inmemory;

import dev.kaldiroglu.wolfdesk.billing.ProductOwners;
import dev.kaldiroglu.wolfdesk.billing.TenantId;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Mimics a table of the products each tenant supports, as billing keeps it. */
public class InMemoryProductOwners implements ProductOwners {

    private final Map<UUID, TenantId> owners = new HashMap<>();

    public void add(UUID productId, TenantId tenant) {
        owners.put(productId, tenant);
    }

    @Override
    public TenantId tenantOf(UUID productId) {
        var tenant = owners.get(productId);
        if (tenant == null) throw new IllegalArgumentException("No tenant supports product " + productId);
        return tenant;
    }
}
