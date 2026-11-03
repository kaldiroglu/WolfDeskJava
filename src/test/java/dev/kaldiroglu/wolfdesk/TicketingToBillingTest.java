package dev.kaldiroglu.wolfdesk;

import dev.kaldiroglu.wolfdesk.billing.businesslogic.RecordTicketOpened;
import dev.kaldiroglu.wolfdesk.billing.dataaccess.InMemoryBillingDatabase;
import dev.kaldiroglu.wolfdesk.billing.dataaccess.InMemoryProductOwners;
import dev.kaldiroglu.wolfdesk.billing.dataaccess.TenantId;
import dev.kaldiroglu.wolfdesk.billing.presentation.TicketingAnticorruptionLayer;
import dev.kaldiroglu.wolfdesk.ticketing.application.OutboxRelay;
import dev.kaldiroglu.wolfdesk.ticketing.application.TicketService;
import dev.kaldiroglu.wolfdesk.ticketing.domain.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.domain.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.ResponseTimeframeCalculationService;
import dev.kaldiroglu.wolfdesk.ticketing.domain.UserId;
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
        bus.subscribe(new TicketingAnticorruptionLayer(new RecordTicketOpened(billing, owners))::on);
        var service = new TicketService(tickets,
                new ResponseTimeframeCalculationService(new InMemoryDepartmentRepository()),
                Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC));

        service.open(UserId.random(), List.of(product), "Cannot log in", Priority.HIGH);
        assertEquals(0, billing.countTicketsOpened(tenant, YearMonth.of(2026, 10)));

        new OutboxRelay(tickets, bus).publishPending();
        assertEquals(1, billing.countTicketsOpened(tenant, YearMonth.of(2026, 10)));
    }
}
