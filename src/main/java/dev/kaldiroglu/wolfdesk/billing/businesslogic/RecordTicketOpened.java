package dev.kaldiroglu.wolfdesk.billing.businesslogic;

import dev.kaldiroglu.wolfdesk.billing.dataaccess.BillingDatabase;
import dev.kaldiroglu.wolfdesk.billing.dataaccess.ProductOwners;

import java.time.Instant;
import java.util.UUID;

/**
 * Records that a tenant's ticket was opened, so that it can be charged. Ticketing knows a
 * ticket's products; billing charges tenants. So the script finds the tenant that supports
 * the ticket's product, and records the ticket for that tenant.
 *
 * <p>The caller may repeat a call it believes has failed (chapter 5, "Implicit distributed
 * transactions"). The ticket's id makes the operation idempotent: recording the same ticket
 * twice counts it once.
 */
public final class RecordTicketOpened {

    private final BillingDatabase db;
    private final ProductOwners productOwners;

    public RecordTicketOpened(BillingDatabase db, ProductOwners productOwners) {
        this.db = db;
        this.productOwners = productOwners;
    }

    public void execute(UUID productId, UUID ticketId, Instant openedAt) {
        try {
            db.startTransaction();
            var tenant = productOwners.tenantOf(productId);
            if (!db.isTicketRecorded(ticketId)) {
                db.insertOpenedTicket(tenant, ticketId, openedAt);
            }
            db.commit();
        } catch (RuntimeException e) {
            db.rollback();
            throw e;
        }
    }
}
