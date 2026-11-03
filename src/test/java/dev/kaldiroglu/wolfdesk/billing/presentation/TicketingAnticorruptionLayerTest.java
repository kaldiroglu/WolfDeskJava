package dev.kaldiroglu.wolfdesk.billing.presentation;

import dev.kaldiroglu.wolfdesk.billing.businesslogic.RecordTicketOpened;
import dev.kaldiroglu.wolfdesk.billing.dataaccess.InMemoryBillingDatabase;
import dev.kaldiroglu.wolfdesk.billing.dataaccess.InMemoryProductOwners;
import dev.kaldiroglu.wolfdesk.billing.dataaccess.TenantId;
import dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage.TicketClosedMessage;
import dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage.TicketOpenedMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TicketingAnticorruptionLayerTest {

    private static final Instant OPENED = Instant.parse("2026-10-20T12:00:00Z");

    private final InMemoryBillingDatabase db = new InMemoryBillingDatabase();
    private final InMemoryProductOwners owners = new InMemoryProductOwners();
    private final TenantId tenant = TenantId.random();
    private final UUID product = UUID.randomUUID();
    private final TicketingAnticorruptionLayer acl =
            new TicketingAnticorruptionLayer(new RecordTicketOpened(db, owners));

    TicketingAnticorruptionLayerTest() {
        owners.add(product, tenant);
    }

    private TicketOpenedMessage opened(UUID ticket) {
        return new TicketOpenedMessage(ticket, List.of(product), "HIGH", OPENED);
    }

    @Test
    void anOpenedTicketIsRecordedForTheTenantOfItsProduct() {
        acl.on(opened(UUID.randomUUID()));

        assertEquals(1, db.countTicketsOpened(tenant, YearMonth.of(2026, 10)));
    }

    @Test
    void aMessageDeliveredTwiceIsCountedOnce() {
        var ticket = UUID.randomUUID();

        acl.on(opened(ticket));
        acl.on(opened(ticket));

        assertEquals(1, db.countTicketsOpened(tenant, YearMonth.of(2026, 10)));
    }

    @Test
    void aMessageBillingDoesNotNeedIsIgnored() {
        acl.on(new TicketClosedMessage(UUID.randomUUID(), OPENED));

        assertEquals(0, db.countTicketsOpened(tenant, YearMonth.of(2026, 10)));
    }
}
