package dev.kaldiroglu.wolfdesk.billing;

import dev.kaldiroglu.wolfdesk.billing.inmemory.InMemoryBillingDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChargeTenantTest {

    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);

    private final TenantId tenant = TenantId.random();
    private InMemoryBillingDatabase db;

    @BeforeEach
    void setUp() {
        db = new InMemoryBillingDatabase();
        db.setPricePerTicket(tenant, new BigDecimal("2.00"));
    }

    private void openTickets(int count, Instant at) {
        var record = new RecordTicketOpened(db);
        for (int i = 0; i < count; i++) record.execute(tenant, UUID.randomUUID(), at);
    }

    @Test
    void chargesEveryTicketOpenedInThePeriodWithItsDiscount() {
        openTickets(501, Instant.parse("2026-10-15T10:00:00Z"));

        var invoice = new ChargeTenant(db).execute(tenant, OCTOBER);

        assertEquals(501, invoice.ticketsOpened());
        assertEquals(10, invoice.discountPercent());
        assertEquals(new BigDecimal("901.80"), invoice.amount());   // 501 x 2.00 x 90%
        assertTrue(db.isPeriodClosed(tenant, OCTOBER));
    }

    @Test
    void ticketsOfAnotherMonthAreNotCharged() {
        openTickets(3, Instant.parse("2026-10-31T23:59:59Z"));
        openTickets(2, Instant.parse("2026-11-01T00:00:00Z"));

        assertEquals(3, new ChargeTenant(db).execute(tenant, OCTOBER).ticketsOpened());
    }

    @Test
    void chargingTheSamePeriodTwiceReturnsTheFirstInvoice() {
        openTickets(10, Instant.parse("2026-10-02T09:00:00Z"));
        var charge = new ChargeTenant(db);

        var first = charge.execute(tenant, OCTOBER);
        openTickets(5, Instant.parse("2026-10-03T09:00:00Z"));
        var second = charge.execute(tenant, OCTOBER);

        assertEquals(first, second);
    }

    @Test
    void aFailureAfterTheInvoiceRollsTheInvoiceBack() {
        var failing = new InMemoryBillingDatabase() {
            @Override
            public void closePeriod(TenantId t, YearMonth p) {
                throw new IllegalStateException("database timeout");
            }
        };
        failing.setPricePerTicket(tenant, BigDecimal.ONE);

        assertThrows(IllegalStateException.class, () -> new ChargeTenant(failing).execute(tenant, OCTOBER));
        assertTrue(failing.findInvoice(tenant, OCTOBER).isEmpty());
    }
}
