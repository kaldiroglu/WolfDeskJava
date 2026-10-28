package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;

/** Command: the customer or the assigned agent writes to the other. */
public record AddMessage(UserId from, String body, Instant at) {
}
