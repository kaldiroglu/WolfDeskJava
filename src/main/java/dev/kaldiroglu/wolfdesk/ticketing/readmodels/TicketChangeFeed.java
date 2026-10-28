package dev.kaldiroglu.wolfdesk.ticketing.readmodels;

import java.util.List;

/**
 * The port a projection reads the command execution model through: the tickets saved
 * after a checkpoint, as in the catch-up subscription of Figures 8-13 and 8-14.
 */
public interface TicketChangeFeed {

    /** The tickets written after {@code checkpoint}, lowest checkpoint first. */
    List<TicketChange> changesAfter(long checkpoint);
}
