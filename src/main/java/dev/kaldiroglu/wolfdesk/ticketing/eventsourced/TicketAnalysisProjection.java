package dev.kaldiroglu.wolfdesk.ticketing.eventsourced;

import dev.kaldiroglu.wolfdesk.ticketing.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.TicketStatus;
import dev.kaldiroglu.wolfdesk.ticketing.eventsourced.TicketEvent.*;

import java.util.List;

/**
 * A second projection of the ticket's events, for analysis (the book's
 * {@code AnalysisModelProjection}, p. 106): how many messages a ticket took, how often it
 * was reassigned, and how it ended. It was added without changing the ticket or its events,
 * and it sees each ticket's whole history.
 */
public final class TicketAnalysisProjection {

    private TicketId ticketId;
    private int messages;
    private int reassignments;
    private boolean escalated;
    private TicketStatus status;
    private int version = -1;

    public static TicketAnalysisProjection of(List<TicketEvent> events) {
        var projection = new TicketAnalysisProjection();
        events.forEach(projection::apply);
        return projection;
    }

    public void apply(TicketEvent event) {
        switch (event) {
            case TicketOpened e -> {
                ticketId = e.ticketId();
                status = TicketStatus.OPEN;
            }
            case MessageAdded e -> messages += 1;
            case TicketEscalated e -> escalated = true;
            case TicketReassigned e -> reassignments += 1;
            case TicketClosed e -> status = TicketStatus.CLOSED;
            case TicketAssigned e -> { }
            case MessageRead e -> { }
        }
        version += 1;
    }

    public TicketId ticketId() {
        return ticketId;
    }

    public int messages() {
        return messages;
    }

    public int reassignments() {
        return reassignments;
    }

    public boolean wasEscalated() {
        return escalated;
    }

    public TicketStatus status() {
        return status;
    }

    public int version() {
        return version;
    }
}
