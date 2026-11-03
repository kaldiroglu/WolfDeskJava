package dev.kaldiroglu.wolfdesk.ticketing.readmodels;

import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketSnapshot;

/** A saved ticket with the checkpoint its row was written at. */
public record TicketChange(long checkpoint, TicketSnapshot ticket) {
}
