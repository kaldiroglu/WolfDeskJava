package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;

/** Command: the customer asks to escalate the ticket. */
public record RequestEscalation(UserId requestedBy, Instant at) {
}
