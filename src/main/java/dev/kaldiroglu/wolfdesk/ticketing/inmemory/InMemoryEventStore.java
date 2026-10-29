package dev.kaldiroglu.wolfdesk.ticketing.inmemory;

import dev.kaldiroglu.wolfdesk.ticketing.ConcurrencyException;
import dev.kaldiroglu.wolfdesk.ticketing.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.eventsourced.EventStore;
import dev.kaldiroglu.wolfdesk.ticketing.eventsourced.TicketEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An append-only event store in memory: each ticket's events in the order they were
 * appended. An append succeeds only if the ticket's last event is the expected version,
 * the version the new events were decided on.
 */
public class InMemoryEventStore implements EventStore {

    private final Map<TicketId, List<TicketEvent>> streams = new HashMap<>();

    @Override
    public synchronized List<TicketEvent> fetch(TicketId id) {
        return List.copyOf(streams.getOrDefault(id, List.of()));
    }

    @Override
    public synchronized void append(TicketId id, List<TicketEvent> newEvents, int expectedVersion) {
        var stream = streams.getOrDefault(id, List.of());
        var actualVersion = stream.size() - 1;
        if (actualVersion != expectedVersion) {
            throw new ConcurrencyException(id, expectedVersion, actualVersion);
        }
        streams.computeIfAbsent(id, key -> new ArrayList<>()).addAll(newEvents);
    }
}
