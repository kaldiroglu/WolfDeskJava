package dev.kaldiroglu.wolfdesk.billing;

import dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage.TicketOpenedMessage;
import dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage.TicketingMessage;

/**
 * Billing's anticorruption layer over ticketing's published language (chapter 9, p. 138):
 * it translates the one message billing needs into billing's own model, and ignores the rest.
 *
 * <p>Ticketing knows a ticket's products; billing charges tenants. So the translation finds
 * the tenant from the ticket's first product. Messages arrive at least once, and
 * {@link RecordTicketOpened} counts a ticket once however often it is delivered.
 */
public final class TicketingAnticorruptionLayer {

    private final ProductOwners productOwners;
    private final RecordTicketOpened recordTicketOpened;

    public TicketingAnticorruptionLayer(ProductOwners productOwners, RecordTicketOpened recordTicketOpened) {
        this.productOwners = productOwners;
        this.recordTicketOpened = recordTicketOpened;
    }

    public void on(TicketingMessage message) {
        if (message instanceof TicketOpenedMessage opened) {
            var tenant = productOwners.tenantOf(opened.products().getFirst());
            recordTicketOpened.execute(tenant, opened.ticketId(), opened.occurredAt());
        }
    }
}
