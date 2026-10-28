package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;
import java.util.Objects;

/**
 * A message inside a ticket. An entity: it has an id, and its state changes when the
 * receiver reads it. Only its ticket can change it.
 */
public final class Message {

    private final MessageId id;
    private final UserId from;
    private final UserId to;
    private final String body;
    private final Instant sentAt;
    private boolean wasRead;

    Message(MessageId id, UserId from, UserId to, String body, Instant sentAt, boolean wasRead) {
        this.id = Objects.requireNonNull(id, "id");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        if (body == null || body.isBlank()) throw new IllegalArgumentException("A message needs a body");
        this.body = body;
        this.sentAt = Objects.requireNonNull(sentAt, "sentAt");
        this.wasRead = wasRead;
    }

    void markAsRead() {
        wasRead = true;
    }

    public MessageId id() {
        return id;
    }

    public UserId from() {
        return from;
    }

    public UserId to() {
        return to;
    }

    public String body() {
        return body;
    }

    public Instant sentAt() {
        return sentAt;
    }

    public boolean wasRead() {
        return wasRead;
    }
}
