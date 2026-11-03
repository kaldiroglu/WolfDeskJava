package dev.kaldiroglu.wolfdesk.ticketing.domain;

import dev.kaldiroglu.wolfdesk.ticketing.domain.events.DomainEvent;

/** One row of the outbox table: a domain event, at its position in the outbox. */
public record OutboxRecord(long position, DomainEvent event) {
}
