package dev.kaldiroglu.wolfdesk.demo;

import dev.kaldiroglu.wolfdesk.billing.businesslogic.ChargeTenant;
import dev.kaldiroglu.wolfdesk.billing.businesslogic.RecordTicketOpened;
import dev.kaldiroglu.wolfdesk.billing.dataaccess.InMemoryBillingDatabase;
import dev.kaldiroglu.wolfdesk.billing.dataaccess.InMemoryProductOwners;
import dev.kaldiroglu.wolfdesk.billing.dataaccess.Invoice;
import dev.kaldiroglu.wolfdesk.billing.dataaccess.TenantId;
import dev.kaldiroglu.wolfdesk.billing.presentation.TicketingAnticorruptionLayer;
import dev.kaldiroglu.wolfdesk.tenants.dataaccess.TenantsDatabase;
import dev.kaldiroglu.wolfdesk.tenants.service.AddSupportedProduct;
import dev.kaldiroglu.wolfdesk.tenants.service.RegisterTenant;
import dev.kaldiroglu.wolfdesk.ticketing.application.ExecutionResult;
import dev.kaldiroglu.wolfdesk.ticketing.application.OutboxRelay;
import dev.kaldiroglu.wolfdesk.ticketing.application.TicketService;
import dev.kaldiroglu.wolfdesk.ticketing.domain.DepartmentPolicy;
import dev.kaldiroglu.wolfdesk.ticketing.domain.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.domain.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.ResponseTimeframeCalculationService;
import dev.kaldiroglu.wolfdesk.ticketing.domain.Shift;
import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.UserId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.events.TicketAssigned;
import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryDepartmentRepository;
import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryEventPublisher;
import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryTicketRepository;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * One story through WolfDesk's three bounded contexts, printed step by step.
 *
 * <p>This class is the composition root: the only place that wires the contexts to their
 * in-memory adapters, as a deployment would wire them to real ones. A tenant registers a
 * product, a customer opens a ticket, the agent misses the response deadline, the customer
 * escalates, and billing charges the tenant once the outbox relay has run.
 */
public final class WolfDeskDemo {

    static final Instant OPENED = Instant.parse("2026-10-28T10:00:00Z");
    static final YearMonth PERIOD = YearMonth.of(2026, 10);
    static final BigDecimal PRICE_PER_TICKET = new BigDecimal("2.00");

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);

    private final PrintStream out;

    public WolfDeskDemo(PrintStream out) {
        this.out = out;
    }

    public static void main(String[] args) {
        new WolfDeskDemo(System.out).run();
    }

    /** Runs the story and returns the tenant's invoice for the period. */
    public Invoice run() {
        // Tenants: a tenant and the product its customers open tickets about.
        var tenantsDb = new TenantsDatabase();
        var tenantId = new RegisterTenant(tenantsDb).execute("Acme", List.of("Login", "Billing"));
        var productId = new AddSupportedProduct(tenantsDb).execute(tenantId, "Acme Mail");
        step("Tenants", "Acme registered with the product Acme Mail");

        // Billing: the tenant's price, and the product's owner for the anticorruption layer.
        var billingDb = new InMemoryBillingDatabase();
        var tenant = new TenantId(tenantId);
        billingDb.setPricePerTicket(tenant, PRICE_PER_TICKET);
        var owners = new InMemoryProductOwners();
        owners.add(productId, tenant);

        // Ticketing: the agent's department policy and shift, the repository with its outbox.
        var agent = UserId.random();
        var customer = UserId.random();
        var departments = new InMemoryDepartmentRepository();
        departments.setPolicy(agent, new DepartmentPolicy(Map.of(Priority.HIGH, Duration.ofHours(2)),
                0.5, Duration.ofDays(1)));
        departments.addShift(agent, new Shift(Instant.parse("2026-10-28T09:00:00Z"),
                Instant.parse("2026-10-28T17:00:00Z")));
        var tickets = new InMemoryTicketRepository();
        var timeframes = new ResponseTimeframeCalculationService(departments);

        // The message bus: billing subscribes to ticketing's published language.
        var bus = new InMemoryEventPublisher();
        bus.subscribe(new TicketingAnticorruptionLayer(new RecordTicketOpened(billingDb, owners))::on);

        var at10 = service(tickets, timeframes, OPENED);
        TicketId ticket = at10.open(customer, List.of(new ProductId(productId)), "Cannot log in",
                Priority.HIGH);
        step("Ticketing", "10:00 The customer opens a high-priority ticket: Cannot log in");
        at10.assign(ticket, agent);
        var deadline = tickets.unpublished().stream()
                .map(record -> record.event())
                .filter(event -> event instanceof TicketAssigned)
                .map(event -> ((TicketAssigned) event).responseDeadline())
                .findFirst().orElseThrow();
        step("Ticketing", "10:00 An agent is assigned; the response deadline is "
                + TIME.format(deadline));
        at10.addMessage(ticket, customer, "I cannot log in since this morning.");
        step("Ticketing", "10:00 The customer writes to the agent");

        var early = service(tickets, timeframes, Instant.parse("2026-10-28T11:00:00Z"))
                .escalate(ticket, customer);
        step("Ticketing", "11:00 The customer asks for an escalation: " + describe(early));

        var late = service(tickets, timeframes, Instant.parse("2026-10-28T12:00:00Z"))
                .escalate(ticket, customer);
        step("Ticketing", "12:00 The customer asks again: " + describe(late));

        service(tickets, timeframes, Instant.parse("2026-10-28T12:30:00Z")).close(ticket, agent);
        step("Ticketing", "12:30 The agent closes the ticket");

        step("Billing", "Tickets counted before the relay runs: "
                + billingDb.countTicketsOpened(tenant, PERIOD));
        new OutboxRelay(tickets, bus).publishPending();
        var published = bus.published().stream().map(m -> m.getClass().getSimpleName()).toList();
        step("Ticketing", "The outbox relay publishes " + String.join(", ", published));
        step("Billing", "Tickets counted after the relay runs: "
                + billingDb.countTicketsOpened(tenant, PERIOD));

        var invoice = new ChargeTenant(billingDb).execute(tenant, PERIOD);
        step("Billing", "Invoice for " + invoice.period() + ": " + invoice.ticketsOpened()
                + " ticket(s) at " + invoice.pricePerTicket() + ", discount "
                + invoice.discountPercent() + "%, amount " + invoice.amount());
        return invoice;
    }

    private static TicketService service(InMemoryTicketRepository tickets,
                                         ResponseTimeframeCalculationService timeframes,
                                         Instant now) {
        return new TicketService(tickets, timeframes, Clock.fixed(now, ZoneOffset.UTC));
    }

    private static String describe(ExecutionResult result) {
        return switch (result) {
            case ExecutionResult.Success s -> "escalated";
            case ExecutionResult.Failure f -> "refused, " + f.reason().toLowerCase();
        };
    }

    private void step(String context, String text) {
        out.printf("%-12s %s%n", "[" + context + "]", text);
    }
}
