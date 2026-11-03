package dev.kaldiroglu.wolfdesk.ticketing.application;

/**
 * The publishing relay of the outbox pattern, pull-based (chapter 9, pp. 145–146): it reads
 * the unpublished events, publishes their public versions, and marks them as published.
 *
 * <p>If it fails after publishing an event but before marking it, the next run publishes
 * the event again: delivery is at least once (p. 147), so consumers must be idempotent.
 */
public final class OutboxRelay {

    private final Outbox outbox;
    private final EventPublisher publisher;

    public OutboxRelay(Outbox outbox, EventPublisher publisher) {
        this.outbox = outbox;
        this.publisher = publisher;
    }

    public void publishPending() {
        for (var record : outbox.unpublished()) {
            PublishedLanguageTranslator.translate(record.event()).ifPresent(publisher::publish);
            outbox.markPublished(record.position());
        }
    }
}
