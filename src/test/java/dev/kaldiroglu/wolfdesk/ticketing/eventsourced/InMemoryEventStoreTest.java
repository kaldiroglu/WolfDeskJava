package dev.kaldiroglu.wolfdesk.ticketing.eventsourced;

import dev.kaldiroglu.wolfdesk.ticketing.ConcurrencyException;
import dev.kaldiroglu.wolfdesk.ticketing.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.UserId;
import dev.kaldiroglu.wolfdesk.ticketing.eventsourced.TicketEvent.*;
import dev.kaldiroglu.wolfdesk.ticketing.inmemory.InMemoryEventStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryEventStoreTest {

    private static final Instant AT = Instant.parse("2026-10-28T20:00:00Z");

    private final InMemoryEventStore store = new InMemoryEventStore();
    private final TicketId id = TicketId.random();
    private final UserId customer = UserId.random();
    private final TicketOpened opened = new TicketOpened(id, customer, List.of(ProductId.random()),
            "Cannot log in", Priority.HIGH, AT);
    private final TicketClosed closed = new TicketClosed(id, customer, AT);

    @Test
    void eventsAreFetchedInTheOrderTheyWereAppended() {
        store.append(id, List.of(opened), -1);
        store.append(id, List.of(closed), 0);

        assertEquals(List.of(opened, closed), store.fetch(id));
    }

    @Test
    void anAppendBasedOnAStaleVersionFails() {
        store.append(id, List.of(opened), -1);
        store.append(id, List.of(closed), 0);

        assertThrows(ConcurrencyException.class,
                () -> store.append(id, List.of(new TicketClosed(id, customer, AT)), 0));
        assertEquals(2, store.fetch(id).size());
    }

    @Test
    void anEarlierFetchIsNotChangedByLaterAppends() {
        store.append(id, List.of(opened), -1);
        var fetched = store.fetch(id);

        store.append(id, List.of(closed), 0);

        assertEquals(List.of(opened), fetched);
    }
}
