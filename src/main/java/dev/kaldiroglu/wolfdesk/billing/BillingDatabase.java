package dev.kaldiroglu.wolfdesk.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

/**
 * The thin abstraction a transaction script may use to reach its storage (chapter 5, p. 64).
 * Changes made after {@link #startTransaction()} become visible only on {@link #commit()};
 * {@link #rollback()} discards them.
 */
public interface BillingDatabase {

    void startTransaction();

    void commit();

    void rollback();

    void setPricePerTicket(TenantId tenant, BigDecimal price);

    Optional<BigDecimal> findPricePerTicket(TenantId tenant);

    boolean isTicketRecorded(UUID ticketId);

    void insertOpenedTicket(TenantId tenant, UUID ticketId, Instant openedAt);

    int countTicketsOpened(TenantId tenant, YearMonth period);

    Optional<Invoice> findInvoice(TenantId tenant, YearMonth period);

    void insertInvoice(Invoice invoice);

    void closePeriod(TenantId tenant, YearMonth period);

    boolean isPeriodClosed(TenantId tenant, YearMonth period);
}
