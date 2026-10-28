package dev.kaldiroglu.wolfdesk.ticketing.inmemory;

import dev.kaldiroglu.wolfdesk.ticketing.DomainEvent;
import dev.kaldiroglu.wolfdesk.ticketing.EventPublisher;

import java.util.ArrayList;
import java.util.List;

/** Mimics a message bus by keeping the published events in a list. */
public class InMemoryEventPublisher implements EventPublisher {

    private final List<DomainEvent> published = new ArrayList<>();

    @Override
    public void publish(DomainEvent event) {
        published.add(event);
    }

    public List<DomainEvent> published() {
        return List.copyOf(published);
    }
}
