package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;

/**
 * Command: apply the rules that depend on time. The application service picks the agent
 * that would take over; the ticket decides whether a reassignment is due.
 */
public record EvaluateAutomaticActions(Instant at, UserId replacementAgent) {
}
