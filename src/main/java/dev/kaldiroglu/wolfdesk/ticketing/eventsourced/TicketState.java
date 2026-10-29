package dev.kaldiroglu.wolfdesk.ticketing.eventsourced;

import dev.kaldiroglu.wolfdesk.ticketing.MessageId;
import dev.kaldiroglu.wolfdesk.ticketing.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.ResponseTimeframe;
import dev.kaldiroglu.wolfdesk.ticketing.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.TicketStatus;
import dev.kaldiroglu.wolfdesk.ticketing.UserId;
import dev.kaldiroglu.wolfdesk.ticketing.eventsourced.TicketEvent.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The ticket's state, projected from its events (the book's {@code TicketState}, p. 110).
 * Each event sets the fields it changes and increments the version; nothing else changes
 * them. The version is -1 before the first event, so {@code TicketOpened} is version 0.
 */
public final class TicketState {

    /** A message as the ticket's events describe it. */
    public record Message(MessageId id, UserId from, UserId to, String body, Instant sentAt,
                          boolean wasRead) {
        Message markedAsRead() {
            return new Message(id, from, to, body, sentAt, true);
        }
    }

    private TicketId id;
    private int version = -1;
    private UserId customer;
    private List<ProductId> products = List.of();
    private String title;
    private Priority priority;
    private UserId assignedAgent;
    private ResponseTimeframe responseTimeframe;
    private boolean escalated;
    private TicketStatus status;
    private final List<Message> messages = new ArrayList<>();

    /** The switch names every kind of event: a new kind does not compile until it is applied here. */
    void apply(TicketEvent event) {
        switch (event) {
            case TicketOpened e -> {
                id = e.ticketId();
                customer = e.customer();
                products = e.products();
                title = e.title();
                priority = e.priority();
                status = TicketStatus.OPEN;
            }
            case TicketAssigned e -> {
                assignedAgent = e.agent();
                responseTimeframe = e.timeframe();
            }
            case MessageAdded e -> messages.add(
                    new Message(e.messageId(), e.from(), e.to(), e.body(), e.occurredAt(), false));
            case MessageRead e -> messages.replaceAll(
                    m -> m.id().equals(e.messageId()) ? m.markedAsRead() : m);
            case TicketEscalated e -> escalated = true;
            case TicketReassigned e -> assignedAgent = e.newAgent();
            case TicketClosed e -> status = TicketStatus.CLOSED;
        }
        version += 1;
    }

    public TicketId id() {
        return id;
    }

    public int version() {
        return version;
    }

    public UserId customer() {
        return customer;
    }

    public List<ProductId> products() {
        return products;
    }

    public String title() {
        return title;
    }

    public Priority priority() {
        return priority;
    }

    public Optional<UserId> assignedAgent() {
        return Optional.ofNullable(assignedAgent);
    }

    public Optional<ResponseTimeframe> responseTimeframe() {
        return Optional.ofNullable(responseTimeframe);
    }

    public boolean isEscalated() {
        return escalated;
    }

    public TicketStatus status() {
        return status;
    }

    public List<Message> messages() {
        return Collections.unmodifiableList(messages);
    }
}
