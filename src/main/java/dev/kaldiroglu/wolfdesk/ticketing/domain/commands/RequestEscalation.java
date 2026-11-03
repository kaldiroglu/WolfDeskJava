package dev.kaldiroglu.wolfdesk.ticketing.domain.commands;

import dev.kaldiroglu.wolfdesk.ticketing.domain.UserId;

import java.time.Instant;

/** Command: the customer asks to escalate the ticket. */
public record RequestEscalation(UserId requestedBy, Instant at) {
}
