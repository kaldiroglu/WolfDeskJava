package dev.kaldiroglu.wolfdesk.ticketing.readmodels;

import dev.kaldiroglu.wolfdesk.ticketing.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.TicketSnapshot;
import dev.kaldiroglu.wolfdesk.ticketing.TicketStatus;
import dev.kaldiroglu.wolfdesk.ticketing.UserId;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Projects the saved tickets into the agents' queues synchronously: each catch-up reads the
 * tickets saved after the last checkpoint, updates the queues, and stores the new
 * checkpoint. Resetting the checkpoint to 0 rebuilds the queues from scratch.
 */
public final class AgentQueueProjection implements AgentQueue {

    private record Row(UserId agent, QueuedTicket ticket) {
    }

    private final TicketChangeFeed feed;
    private final Map<TicketId, Row> rows = new HashMap<>();
    private long checkpoint;

    public AgentQueueProjection(TicketChangeFeed feed) {
        this.feed = feed;
    }

    /** Applies the changes after the last checkpoint and returns how many there were. */
    public synchronized int catchUp() {
        var changes = feed.changesAfter(checkpoint);
        for (var change : changes) {
            project(change.ticket());
            checkpoint = change.checkpoint();
        }
        return changes.size();
    }

    /** Wipes the queues; the next catch-up projects every ticket again. */
    public synchronized void reset() {
        rows.clear();
        checkpoint = 0;
    }

    public synchronized long checkpoint() {
        return checkpoint;
    }

    @Override
    public synchronized List<QueuedTicket> openTicketsOf(UserId agent) {
        return rows.values().stream()
                .filter(row -> row.agent().equals(agent))
                .map(Row::ticket)
                .sorted(Comparator.comparing(QueuedTicket::deadline))
                .toList();
    }

    private void project(TicketSnapshot ticket) {
        if (ticket.status() != TicketStatus.OPEN || ticket.assignedAgent() == null) {
            rows.remove(ticket.id());
            return;
        }
        rows.put(ticket.id(), new Row(ticket.assignedAgent(), new QueuedTicket(ticket.id(),
                ticket.title(), ticket.priority(), ticket.responseTimeframe().deadline(),
                ticket.escalated())));
    }
}
