package dev.kaldiroglu.wolfdesk.ticketing;

import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryTicketRepository;
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
    void anUnknownTicketIsNotFound() {
        assertThrows(TicketNotFoundException.class, () -> repository.load(TicketId.random()));
    }
}
