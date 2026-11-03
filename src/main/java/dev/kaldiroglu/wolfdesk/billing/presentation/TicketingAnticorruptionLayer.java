package dev.kaldiroglu.wolfdesk.billing.presentation;

import dev.kaldiroglu.wolfdesk.billing.businesslogic.RecordTicketOpened;
import dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage.TicketOpenedMessage;
import dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage.TicketingMessage;

/**
 * Billing's anticorruption layer over ticketing's published language (chapter 9, p. 138):
 * it translates the one message billing needs into billing's own model, and ignores the rest.
 *
 * <p>It belongs to the presentation layer: a subscription to a message broker is one of the
 * ways a system receives requests (chapter 8, p. 119). It translates the message into a call
 * of a transaction script in the layer beneath, and nothing else: the script finds the
 * tenant of the ticket's first product. Messages arrive at least once, and
 * {@link RecordTicketOpened} counts a ticket once however often it is delivered.
 */
public final class TicketingAnticorruptionLayer {

    private final RecordTicketOpened recordTicketOpened;

    public TicketingAnticorruptionLayer(RecordTicketOpened recordTicketOpened) {
        this.recordTicketOpened = recordTicketOpened;
    }

    public void on(TicketingMessage message) {
        if (message instanceof TicketOpenedMessage opened) {
            recordTicketOpened.execute(opened.products().getFirst(), opened.ticketId(), opened.occurredAt());
        }
    }
}
