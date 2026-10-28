package dev.kaldiroglu.wolfdesk.ticketing.inmemory;

import dev.kaldiroglu.wolfdesk.ticketing.ConcurrencyException;
import dev.kaldiroglu.wolfdesk.ticketing.Ticket;
import dev.kaldiroglu.wolfdesk.ticketing.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.TicketNotFoundException;
import dev.kaldiroglu.wolfdesk.ticketing.TicketRepository;
import dev.kaldiroglu.wolfdesk.ticketing.readmodels.TicketChange;
import dev.kaldiroglu.wolfdesk.ticketing.readmodels.TicketChangeFeed;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mimics a tickets table with a version column. A save succeeds only if the stored version
 * is the one the ticket was loaded with, and then increments it, like listing 06-12:
 * {@code UPDATE tickets ... WHERE ticket_id=@id and agg_version=@expected_version}.
 *
 * <p>Each save also stamps the row with the next checkpoint, like SQL Server's
 * {@code rowversion} column (Figure 8-15), so the read models can catch up on the changes.
 */
public class InMemoryTicketRepository implements TicketRepository, TicketChangeFeed {

    private final Map<TicketId, TicketChange> rows = new HashMap<>();
    private long lastCheckpoint;

    @Override
    public synchronized Ticket load(TicketId id) {
        var row = rows.get(id);
        if (row == null) throw new TicketNotFoundException(id);
        return Ticket.restore(row.ticket());
    }

    @Override
    public synchronized void save(Ticket ticket) {
        var snapshot = ticket.snapshot();
        var stored = rows.get(snapshot.id());
        var storedVersion = stored == null ? 0 : stored.ticket().version();
        if (storedVersion != snapshot.version()) {
            throw new ConcurrencyException(snapshot.id(), snapshot.version(), storedVersion);
        }
        lastCheckpoint++;
        rows.put(snapshot.id(), new TicketChange(lastCheckpoint, snapshot.withVersion(storedVersion + 1)));
    }

    @Override
    public synchronized List<TicketChange> changesAfter(long checkpoint) {
        return rows.values().stream()
                .filter(row -> row.checkpoint() > checkpoint)
                .sorted(Comparator.comparingLong(TicketChange::checkpoint))
                .toList();
    }
}
