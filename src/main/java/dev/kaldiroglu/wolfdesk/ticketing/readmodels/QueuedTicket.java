package dev.kaldiroglu.wolfdesk.ticketing.readmodels;

import dev.kaldiroglu.wolfdesk.ticketing.domain.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketId;

import java.time.Instant;

/** One open ticket as an agent's queue shows it. */
public record QueuedTicket(TicketId id, String title, Priority priority, Instant deadline,
                           boolean escalated) {
}
