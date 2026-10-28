package dev.kaldiroglu.wolfdesk.ticketing;

/** Publishes a ticket's domain events to the rest of the system. */
public interface EventPublisher {

    void publish(DomainEvent event);
}
