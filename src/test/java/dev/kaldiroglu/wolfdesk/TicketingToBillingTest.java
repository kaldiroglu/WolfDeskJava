package dev.kaldiroglu.wolfdesk;

import dev.kaldiroglu.wolfdesk.billing.RecordTicketOpened;
import dev.kaldiroglu.wolfdesk.billing.TenantId;
import dev.kaldiroglu.wolfdesk.billing.TicketingAnticorruptionLayer;
import dev.kaldiroglu.wolfdesk.billing.inmemory.InMemoryBillingDatabase;
import dev.kaldiroglu.wolfdesk.billing.inmemory.InMemoryProductOwners;
import dev.kaldiroglu.wolfdesk.ticketing.OutboxRelay;
import dev.kaldiroglu.wolfdesk.ticketing.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.ResponseTimeframeCalculationService;
import dev.kaldiroglu.wolfdesk.ticketing.TicketService;
import dev.kaldiroglu.wolfdesk.ticketing.UserId;
import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryDepartmentRepository;
import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryEventPublisher;
import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryTicketRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Two bounded contexts, wired the way a deployment would: ticketing's outbox to billing. */
class TicketingToBillingTest {

    @Test
    void aTicketOpenedInTicketingIsCountedByBillingOnceTheRelayRuns() {
        var tickets = new InMemoryTicketRepository();
        var bus = new InMemoryEventPublisher();
        var billing = new InMemoryBillingDatabase();
        var owners = new InMemoryProductOwners();
        var tenant = TenantId.random();
        var product = ProductId.random();
        owners.add(product.value(), tenant);
        bus.subscribe(new TicketingAnticorruptionLayer(owners, new RecordTicketOpened(billing))::on);
        var service = new TicketService(tickets,
                new ResponseTimeframeCalculationService(new InMemoryDepartmentRepository()),
                Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC));

        service.open(UserId.random(), List.of(product), "Cannot log in", Priority.HIGH);
        assertEquals(0, billing.countTicketsOpened(tenant, YearMonth.of(2026, 10)));

        new OutboxRelay(tickets, bus).publishPending();
        assertEquals(1, billing.countTicketsOpened(tenant, YearMonth.of(2026, 10)));
    }
}
