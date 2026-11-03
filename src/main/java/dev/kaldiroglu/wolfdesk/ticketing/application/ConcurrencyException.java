package dev.kaldiroglu.wolfdesk.ticketing.application;

import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketId;

/** Someone else saved the ticket after it was loaded. */
public class ConcurrencyException extends RuntimeException {

    public ConcurrencyException(TicketId id, int expectedVersion, int actualVersion) {
        super("Ticket " + id.value() + " is at version " + actualVersion
                + ", not " + expectedVersion);
    }
}
