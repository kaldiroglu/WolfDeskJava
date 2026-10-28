package dev.kaldiroglu.wolfdesk.billing.inmemory;

import dev.kaldiroglu.wolfdesk.billing.BillingDatabase;
import dev.kaldiroglu.wolfdesk.billing.Invoice;
import dev.kaldiroglu.wolfdesk.billing.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Mimics a relational database with maps. A transaction works on a copy of the tables;
 * commit replaces the tables with the copy, and rollback throws the copy away.
 * Charging periods are calendar months in UTC.
 */
public class InMemoryBillingDatabase implements BillingDatabase {

    private record OpenedTicket(TenantId tenant, Instant openedAt) {
    }

    private record PeriodKey(TenantId tenant, YearMonth period) {
    }

    private static final class Tables {
        final Map<TenantId, BigDecimal> prices = new HashMap<>();
        final Map<UUID, OpenedTicket> openedTickets = new HashMap<>();
        final Map<PeriodKey, Invoice> invoices = new HashMap<>();
        final Set<PeriodKey> closedPeriods = new HashSet<>();

        Tables copy() {
            var copy = new Tables();
            copy.prices.putAll(prices);
            copy.openedTickets.putAll(openedTickets);
            copy.invoices.putAll(invoices);
            copy.closedPeriods.addAll(closedPeriods);
            return copy;
        }
    }

    private Tables committed = new Tables();
    private Tables working;

    @Override
    public void startTransaction() {
        if (working != null) throw new IllegalStateException("A transaction is already open");
        working = committed.copy();
    }

    @Override
    public void commit() {
        committed = tables();
        working = null;
    }

    @Override
    public void rollback() {
        working = null;
    }

    /** Outside a transaction, reads and writes go straight to the committed tables. */
    private Tables tables() {
        return working != null ? working : committed;
    }

    @Override
    public void setPricePerTicket(TenantId tenant, BigDecimal price) {
        tables().prices.put(tenant, price);
    }

    @Override
    public Optional<BigDecimal> findPricePerTicket(TenantId tenant) {
        return Optional.ofNullable(tables().prices.get(tenant));
    }

    @Override
    public boolean isTicketRecorded(UUID ticketId) {
        return tables().openedTickets.containsKey(ticketId);
    }

    @Override
    public void insertOpenedTicket(TenantId tenant, UUID ticketId, Instant openedAt) {
        if (tables().openedTickets.putIfAbsent(ticketId, new OpenedTicket(tenant, openedAt)) != null) {
            throw new IllegalStateException("Duplicate key: ticket " + ticketId);
        }
    }

    @Override
    public int countTicketsOpened(TenantId tenant, YearMonth period) {
        return (int) tables().openedTickets.values().stream()
                .filter(t -> t.tenant().equals(tenant))
                .filter(t -> YearMonth.from(t.openedAt().atZone(ZoneOffset.UTC)).equals(period))
                .count();
    }

    @Override
    public Optional<Invoice> findInvoice(TenantId tenant, YearMonth period) {
        return Optional.ofNullable(tables().invoices.get(new PeriodKey(tenant, period)));
    }

    @Override
    public void insertInvoice(Invoice invoice) {
        var key = new PeriodKey(invoice.tenant(), invoice.period());
        if (tables().invoices.putIfAbsent(key, invoice) != null) {
            throw new IllegalStateException("Duplicate key: invoice " + key);
        }
    }

    @Override
    public void closePeriod(TenantId tenant, YearMonth period) {
        tables().closedPeriods.add(new PeriodKey(tenant, period));
    }

    @Override
    public boolean isPeriodClosed(TenantId tenant, YearMonth period) {
        return tables().closedPeriods.contains(new PeriodKey(tenant, period));
    }
}
