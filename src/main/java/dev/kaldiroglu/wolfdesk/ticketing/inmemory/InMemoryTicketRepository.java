package dev.kaldiroglu.wolfdesk.ticketing.inmemory;

import dev.kaldiroglu.wolfdesk.ticketing.domain.ConcurrencyException;
import dev.kaldiroglu.wolfdesk.ticketing.domain.Outbox;
import dev.kaldiroglu.wolfdesk.ticketing.domain.OutboxRecord;
import dev.kaldiroglu.wolfdesk.ticketing.domain.Ticket;
import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketNotFoundException;
import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketRepository;
import dev.kaldiroglu.wolfdesk.ticketing.domain.events.DomainEvent;
import dev.kaldiroglu.wolfdesk.ticketing.readmodels.TicketChange;
import dev.kaldiroglu.wolfdesk.ticketing.readmodels.TicketChangeFeed;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mimics a tickets table with a version column. A save succeeds only if the stored version
 * is the one the ticket was loaded with, and then increments it, like listing 06-12:
 * {@code UPDATE tickets ... WHERE ticket_id=@id and agg_version=@expected_version}.
 *
 * <p>Each save also stamps the row with the next checkpoint, like SQL Server's
 * {@code rowversion} column (Figure 8-15), so the read models can catch up on the changes.
 *
 * <p>The ticket's new domain events go to an outbox table in the same save, so the ticket
 * and its events are committed together (chapter 9, p. 145).
 */
public class InMemoryTicketRepository implements TicketRepository, TicketChangeFeed, Outbox {

    private final Map<TicketId, TicketChange> rows = new HashMap<>();
    private final TreeMap<Long, DomainEvent> outbox = new TreeMap<>();
    private long lastCheckpoint;
    private long lastOutboxPosition;

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
        ticket.pullDomainEvents().forEach(event -> outbox.put(++lastOutboxPosition, event));
    }

    @Override
    public synchronized List<OutboxRecord> unpublished() {
        return outbox.entrySet().stream().map(e -> new OutboxRecord(e.getKey(), e.getValue())).toList();
    }

    @Override
    public synchronized void markPublished(long position) {
        outbox.remove(position);
    }

    @Override
    public synchronized List<TicketChange> changesAfter(long checkpoint) {
        return rows.values().stream()
                .filter(row -> row.checkpoint() > checkpoint)
                .sorted(Comparator.comparingLong(TicketChange::checkpoint))
                .toList();
    }
}
