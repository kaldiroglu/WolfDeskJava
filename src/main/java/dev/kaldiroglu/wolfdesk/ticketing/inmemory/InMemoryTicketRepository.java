package dev.kaldiroglu.wolfdesk.ticketing.inmemory;

import dev.kaldiroglu.wolfdesk.ticketing.ConcurrencyException;
import dev.kaldiroglu.wolfdesk.ticketing.Ticket;
import dev.kaldiroglu.wolfdesk.ticketing.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.TicketNotFoundException;
import dev.kaldiroglu.wolfdesk.ticketing.TicketRepository;
import dev.kaldiroglu.wolfdesk.ticketing.TicketSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Mimics a tickets table with a version column. A save succeeds only if the stored version
 * is the one the ticket was loaded with, and then increments it, like listing 06-12:
 * {@code UPDATE tickets ... WHERE ticket_id=@id and agg_version=@expected_version}.
 */
public class InMemoryTicketRepository implements TicketRepository {

    private final Map<TicketId, TicketSnapshot> rows = new HashMap<>();

    @Override
    public synchronized Ticket load(TicketId id) {
        var row = rows.get(id);
        if (row == null) throw new TicketNotFoundException(id);
        return Ticket.restore(row);
    }

    @Override
    public synchronized void save(Ticket ticket) {
        var snapshot = ticket.snapshot();
        var stored = rows.get(snapshot.id());
        var storedVersion = stored == null ? 0 : stored.version();
        if (storedVersion != snapshot.version()) {
            throw new ConcurrencyException(snapshot.id(), snapshot.version(), storedVersion);
        }
        rows.put(snapshot.id(), snapshot.withVersion(storedVersion + 1));
    }
}
