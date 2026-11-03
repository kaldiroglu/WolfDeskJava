package dev.kaldiroglu.wolfdesk.billing.businesslogic;

import dev.kaldiroglu.wolfdesk.billing.dataaccess.InMemoryBillingDatabase;
import dev.kaldiroglu.wolfdesk.billing.dataaccess.InMemoryProductOwners;
import dev.kaldiroglu.wolfdesk.billing.dataaccess.TenantId;
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
        var product = UUID.randomUUID();
        var owners = new InMemoryProductOwners();
        owners.add(product, tenant);
        var ticket = UUID.randomUUID();
        var at = Instant.parse("2026-10-20T12:00:00Z");
        var record = new RecordTicketOpened(db, owners);

        record.execute(product, ticket, at);
        record.execute(product, ticket, at);   // the caller retried

        assertEquals(1, db.countTicketsOpened(tenant, YearMonth.of(2026, 10)));
    }
}
