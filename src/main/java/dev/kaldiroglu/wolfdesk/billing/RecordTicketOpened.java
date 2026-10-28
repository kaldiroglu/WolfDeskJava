package dev.kaldiroglu.wolfdesk.billing;

import java.time.Instant;
import java.util.UUID;

/**
 * Records that a tenant's ticket was opened, so that it can be charged.
 *
 * <p>The caller may repeat a call it believes has failed (chapter 5, "Implicit distributed
 * transactions"). The ticket's id makes the operation idempotent: recording the same ticket
 * twice counts it once.
 */
public final class RecordTicketOpened {

    private final BillingDatabase db;

    public RecordTicketOpened(BillingDatabase db) {
        this.db = db;
    }

    public void execute(TenantId tenant, UUID ticketId, Instant openedAt) {
        try {
            db.startTransaction();
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
