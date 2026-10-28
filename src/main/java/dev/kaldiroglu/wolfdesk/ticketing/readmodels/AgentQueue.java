package dev.kaldiroglu.wolfdesk.ticketing.readmodels;

import dev.kaldiroglu.wolfdesk.ticketing.UserId;

import java.util.List;

/** A read model: each agent's open tickets. It has queries only; no command changes it. */
public interface AgentQueue {

    /** The agent's open tickets, earliest deadline first. */
    List<QueuedTicket> openTicketsOf(UserId agent);
}
