package dev.kaldiroglu.wolfdesk.tenants;

import java.util.UUID;

/** A transaction script that adds a product to the list a tenant supports. */
public final class AddSupportedProduct {

    private final TenantsDatabase db;

    public AddSupportedProduct(TenantsDatabase db) {
        this.db = db;
    }

    public UUID execute(UUID tenantId, String productName) {
        try {
            db.startTransaction();

            var tenant = Tenant.find(db, tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("No tenant " + tenantId));
            var product = new Product(productName);
            tenant.getProducts().add(product);
            tenant.save();

            db.commit();
            return product.getId();
        } catch (RuntimeException e) {
            db.rollback();
            throw e;
        }
    }
}
