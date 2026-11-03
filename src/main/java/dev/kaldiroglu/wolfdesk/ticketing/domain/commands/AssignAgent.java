package dev.kaldiroglu.wolfdesk.ticketing.domain.commands;

import dev.kaldiroglu.wolfdesk.ticketing.domain.ResponseTimeframe;
import dev.kaldiroglu.wolfdesk.ticketing.domain.UserId;

import java.time.Instant;

/** Command: give the ticket to an agent, with the time the agent has to respond. */
public record AssignAgent(UserId agent, ResponseTimeframe timeframe, Instant at) {
}
