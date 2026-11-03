package dev.kaldiroglu.wolfdesk.ticketing.domain.commands;

import dev.kaldiroglu.wolfdesk.ticketing.domain.UserId;

import java.time.Instant;

/** Command: the customer or the agent closes the ticket. */
public record CloseTicket(UserId closedBy, Instant at) {
}
