package dev.kaldiroglu.wolfdesk.tenants;

import java.util.List;
import java.util.UUID;

/** A transaction script that creates a tenant through its active record. */
public final class RegisterTenant {

    private final TenantsDatabase db;

    public RegisterTenant(TenantsDatabase db) {
        this.db = db;
    }

    public UUID execute(String name, List<String> ticketCategories) {
        try {
            db.startTransaction();

            var tenant = new Tenant(db);
            tenant.setName(name);
            tenant.getTicketCategories().addAll(ticketCategories);
            tenant.save();

            db.commit();
            return tenant.getId();
        } catch (RuntimeException e) {
            db.rollback();
            throw e;
        }
    }
}
