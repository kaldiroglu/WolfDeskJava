package dev.kaldiroglu.wolfdesk.ticketing.inmemory;

import dev.kaldiroglu.wolfdesk.ticketing.EventPublisher;
import dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage.TicketingMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Mimics a message bus: it keeps the published messages and hands each to the subscribers. */
public class InMemoryEventPublisher implements EventPublisher {

    private final List<TicketingMessage> published = new ArrayList<>();
    private final List<Consumer<TicketingMessage>> subscribers = new ArrayList<>();

    @Override
    public void publish(TicketingMessage message) {
        published.add(message);
        subscribers.forEach(subscriber -> subscriber.accept(message));
    }

    public void subscribe(Consumer<TicketingMessage> subscriber) {
        subscribers.add(subscriber);
    }

    public List<TicketingMessage> published() {
        return List.copyOf(published);
    }
}
