package dev.kaldiroglu.wolfdesk.ticketing;

/** One row of the outbox table: a domain event, at its position in the outbox. */
public record OutboxRecord(long position, DomainEvent event) {
}
