package dev.kaldiroglu.wolfdesk.ticketing;

/** A command was rejected because it would break one of the ticket's rules. */
public class TicketRuleViolationException extends RuntimeException {

    public TicketRuleViolationException(String message) {
        super(message);
    }
}
