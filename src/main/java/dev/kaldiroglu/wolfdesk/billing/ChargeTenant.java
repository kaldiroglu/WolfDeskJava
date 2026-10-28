package dev.kaldiroglu.wolfdesk.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

/**
 * Charges a tenant for the tickets opened in one charging period, with the volume discount.
 *
 * <p>Two writes, the invoice and the closed period, run in one transaction: either both
 * happen or neither does. Charging a period that already has an invoice returns that
 * invoice and changes nothing, so a repeated call cannot charge twice.
 */
public final class ChargeTenant {

    private final BillingDatabase db;

    public ChargeTenant(BillingDatabase db) {
        this.db = db;
    }

    public Invoice execute(TenantId tenant, YearMonth period) {
        try {
            db.startTransaction();

            var existing = db.findInvoice(tenant, period);
            if (existing.isPresent()) {
                db.commit();
                return existing.get();
            }

            var tickets = db.countTicketsOpened(tenant, period);
            var price = db.findPricePerTicket(tenant)
                    .orElseThrow(() -> new IllegalStateException("No price for " + tenant));
            var discount = VolumeDiscount.percentFor(tickets);
            var amount = price.multiply(BigDecimal.valueOf(tickets))
                    .multiply(BigDecimal.valueOf(100 - discount))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            var invoice = new Invoice(tenant, period, tickets, price, discount, amount);
            db.insertInvoice(invoice);
            db.closePeriod(tenant, period);

            db.commit();
            return invoice;
        } catch (RuntimeException e) {
            db.rollback();
            throw e;
        }
    }
}
