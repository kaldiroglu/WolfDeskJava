package dev.kaldiroglu.wolfdesk.ticketing.application;

import dev.kaldiroglu.wolfdesk.ticketing.domain.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.domain.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.ResponseTimeframe;
import dev.kaldiroglu.wolfdesk.ticketing.domain.Ticket;
import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.UserId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.commands.AssignAgent;
import dev.kaldiroglu.wolfdesk.ticketing.domain.commands.CloseTicket;
import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryEventPublisher;
import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryTicketRepository;
import dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage.TicketClosedMessage;
import dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage.TicketOpenedMessage;
import dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage.TicketingMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-10-28T20:00:00Z");

    private final InMemoryTicketRepository tickets = new InMemoryTicketRepository();
    private final InMemoryEventPublisher bus = new InMemoryEventPublisher();
    private final UserId customer = UserId.random();
    private final ProductId product = ProductId.random();

    private Ticket openAssignAndClose() {
        var ticket = Ticket.open(TicketId.random(), customer, List.of(product), "Cannot log in", Priority.HIGH, NOW);
        ticket.execute(new AssignAgent(UserId.random(), new ResponseTimeframe(NOW, NOW.plusSeconds(3600)), NOW));
        ticket.execute(new CloseTicket(customer, NOW));
        tickets.save(ticket);
        return ticket;
    }

    /** Publishes through the given publisher, but fails on the given call. */
    private static EventPublisher failingOn(EventPublisher inner, int failingCall) {
        return new EventPublisher() {
            private int calls;

            @Override
            public void publish(TicketingMessage message) {
                if (++calls == failingCall) throw new IllegalStateException("The message bus is down");
                inner.publish(message);
            }
        };
    }

    @Test
    void onlyThePublicEventsArePublishedInThePublishedLanguage() {
        var ticket = openAssignAndClose();

        new OutboxRelay(tickets, bus).publishPending();

        assertEquals(List.of(TicketOpenedMessage.class, TicketClosedMessage.class),
                bus.published().stream().map(Object::getClass).toList());
        var opened = (TicketOpenedMessage) bus.published().getFirst();
        assertEquals(ticket.id().value(), opened.ticketId());
        assertEquals(List.of(product.value()), opened.products());
        assertTrue(tickets.unpublished().isEmpty());
    }

    @Test
    void anEventThatFailedToPublishStaysInTheOutboxForTheNextRun() {
        openAssignAndClose();
        var relay = new OutboxRelay(tickets, failingOn(bus, 1));

        assertThrows(IllegalStateException.class, relay::publishPending);
        assertEquals(3, tickets.unpublished().size());

        relay.publishPending();
        assertEquals(2, bus.published().size());
        assertTrue(tickets.unpublished().isEmpty());
    }

    @Test
    void anEventIsPublishedAgainIfTheRelayStopsBeforeMarkingIt() {
        openAssignAndClose();
        // The relay publishes the first event, then fails before marking it as published.
        var stopsOnce = new Outbox() {
            private boolean failed;

            @Override
            public List<OutboxRecord> unpublished() {
                return tickets.unpublished();
            }

            @Override
            public void markPublished(long position) {
                if (!failed) {
                    failed = true;
                    throw new IllegalStateException("The relay stopped");
                }
                tickets.markPublished(position);
            }
        };
        var relay = new OutboxRelay(stopsOnce, bus);

        assertThrows(IllegalStateException.class, relay::publishPending);
        relay.publishPending();

        assertEquals(List.of(TicketOpenedMessage.class, TicketOpenedMessage.class, TicketClosedMessage.class),
                bus.published().stream().map(Object::getClass).toList());
    }
}
