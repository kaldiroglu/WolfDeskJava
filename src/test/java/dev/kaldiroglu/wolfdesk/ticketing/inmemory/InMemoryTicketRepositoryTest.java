package dev.kaldiroglu.wolfdesk.ticketing.inmemory;

import dev.kaldiroglu.wolfdesk.ticketing.application.ConcurrencyException;
import dev.kaldiroglu.wolfdesk.ticketing.application.OutboxRecord;
import dev.kaldiroglu.wolfdesk.ticketing.application.TicketNotFoundException;
import dev.kaldiroglu.wolfdesk.ticketing.domain.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.domain.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.Ticket;
import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.UserId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.commands.CloseTicket;
import dev.kaldiroglu.wolfdesk.ticketing.domain.events.TicketOpened;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTicketRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-10-28T20:00:00Z");

    private final InMemoryTicketRepository repository = new InMemoryTicketRepository();

    private Ticket newTicket() {
        return Ticket.open(TicketId.random(), UserId.random(), List.of(ProductId.random()),
                "Cannot log in", Priority.LOW, NOW);
    }

    @Test
    void eachSaveIncrementsTheVersion() {
        var ticket = newTicket();
        repository.save(ticket);
        assertEquals(1, repository.load(ticket.id()).version());

        repository.save(repository.load(ticket.id()));
        assertEquals(2, repository.load(ticket.id()).version());
    }

    @Test
    void aTicketChangedSinceItWasLoadedIsNotSaved() {
        var ticket = newTicket();
        repository.save(ticket);
        var first = repository.load(ticket.id());
        var second = repository.load(ticket.id());

        repository.save(first);

        assertThrows(ConcurrencyException.class, () -> repository.save(second));
    }

    @Test
    void everySaveTakesTheNextCheckpoint() {
        var first = newTicket();
        var second = newTicket();
        repository.save(first);
        repository.save(second);

        var checkpoints = repository.changesAfter(0).stream().map(c -> c.checkpoint()).toList();
        assertEquals(List.of(1L, 2L), checkpoints);
    }

    @Test
    void aTicketSavedTwiceIsOneChangeAtItsLatestCheckpoint() {
        var ticket = newTicket();
        repository.save(ticket);
        repository.save(repository.load(ticket.id()));

        var changes = repository.changesAfter(0);
        assertEquals(1, changes.size());
        assertEquals(2L, changes.getFirst().checkpoint());
        assertEquals(List.of(), repository.changesAfter(2));
    }

    @Test
    void anUnknownTicketIsNotFound() {
        assertThrows(TicketNotFoundException.class, () -> repository.load(TicketId.random()));
    }

    @Test
    void aSaveKeepsTheTicketsNewEventsInTheOutbox() {
        var ticket = newTicket();
        repository.save(ticket);

        var events = repository.unpublished().stream().map(OutboxRecord::event).toList();
        assertEquals(1, events.size());
        assertInstanceOf(TicketOpened.class, events.getFirst());
    }

    @Test
    void aRejectedSaveKeepsNoEventsInTheOutbox() {
        var customer = UserId.random();
        var ticket = Ticket.open(TicketId.random(), customer, List.of(ProductId.random()),
                "Cannot log in", Priority.LOW, NOW);
        repository.save(ticket);
        var first = repository.load(ticket.id());
        var second = repository.load(ticket.id());
        repository.save(first);
        second.execute(new CloseTicket(customer, NOW));

        assertThrows(ConcurrencyException.class, () -> repository.save(second));
        assertEquals(1, repository.unpublished().size());
    }

    @Test
    void aPublishedEventLeavesTheOutbox() {
        repository.save(newTicket());
        repository.save(newTicket());

        repository.markPublished(repository.unpublished().getFirst().position());

        assertEquals(List.of(2L), repository.unpublished().stream().map(OutboxRecord::position).toList());
    }
}
