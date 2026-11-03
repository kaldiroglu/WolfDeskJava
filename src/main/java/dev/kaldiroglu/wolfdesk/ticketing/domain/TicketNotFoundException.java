package dev.kaldiroglu.wolfdesk.ticketing.domain;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(TicketId id) {
        super("No ticket " + id.value());
    }
}
