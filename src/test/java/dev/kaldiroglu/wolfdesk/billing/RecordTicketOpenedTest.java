package dev.kaldiroglu.wolfdesk.billing;

import dev.kaldiroglu.wolfdesk.billing.inmemory.InMemoryBillingDatabase;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecordTicketOpenedTest {

    @Test
    void recordingTheSameTicketTwiceCountsItOnce() {
        var db = new InMemoryBillingDatabase();
        var tenant = TenantId.random();
        var ticket = UUID.randomUUID();
        var at = Instant.parse("2026-10-20T12:00:00Z");
        var record = new RecordTicketOpened(db);

        record.execute(tenant, ticket, at);
        record.execute(tenant, ticket, at);   // the caller retried

        assertEquals(1, db.countTicketsOpened(tenant, YearMonth.of(2026, 10)));
    }
}
