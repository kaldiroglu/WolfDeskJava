package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;

/** Command: the customer or the agent closes the ticket. */
public record CloseTicket(UserId closedBy, Instant at) {
}
