package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;

/** Command: give the ticket to an agent, with the time the agent has to respond. */
public record AssignAgent(UserId agent, ResponseTimeframe timeframe, Instant at) {
}
