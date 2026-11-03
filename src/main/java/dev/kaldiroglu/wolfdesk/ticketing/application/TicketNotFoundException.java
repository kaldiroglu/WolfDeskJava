package dev.kaldiroglu.wolfdesk.ticketing.application;

import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketId;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(TicketId id) {
        super("No ticket " + id.value());
    }
}
