package dev.kaldiroglu.wolfdesk.ticketing.application;

import dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage.TicketingMessage;

/** Publishes ticketing's public messages to the rest of the system, through a message bus. */
public interface EventPublisher {

    void publish(TicketingMessage message);
}
