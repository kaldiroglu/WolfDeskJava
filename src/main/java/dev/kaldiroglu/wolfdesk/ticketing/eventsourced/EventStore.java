package dev.kaldiroglu.wolfdesk.ticketing.eventsourced;

import dev.kaldiroglu.wolfdesk.ticketing.ConcurrencyException;
import dev.kaldiroglu.wolfdesk.ticketing.TicketId;

import java.util.List;

/**
 * The event store (the book's {@code IEventStore}, p. 107). It is append-only: it fetches a
 * ticket's events and appends new ones, and offers no way to change or delete an event.
 * {@link #append} fails with {@link ConcurrencyException} if events were appended after
 * {@code expectedVersion}, the version the new events are based on.
 */
public interface EventStore {
    List<TicketEvent> fetch(TicketId id);

    void append(TicketId id, List<TicketEvent> newEvents, int expectedVersion);
}
