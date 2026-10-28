package dev.kaldiroglu.wolfdesk.ticketing;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(TicketId id) {
        super("No ticket " + id.value());
    }
}
