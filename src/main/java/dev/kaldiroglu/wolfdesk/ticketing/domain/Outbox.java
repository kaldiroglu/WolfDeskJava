package dev.kaldiroglu.wolfdesk.ticketing.domain;

import java.util.List;

/**
 * The outbox (chapter 9, p. 145): the domain events saved in the same transaction as their
 * ticket, kept until the relay has published them.
 */
public interface Outbox {

    /** The events not yet published, oldest first. */
    List<OutboxRecord> unpublished();

    void markPublished(long position);
}
