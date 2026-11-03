package dev.kaldiroglu.wolfdesk.billing;

import java.util.UUID;

/** Which tenant supports a product: what billing needs to know about tenants' products. */
public interface ProductOwners {

    TenantId tenantOf(UUID productId);
}
