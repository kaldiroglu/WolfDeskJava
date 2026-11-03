package dev.kaldiroglu.wolfdesk.ticketing.application;

import dev.kaldiroglu.wolfdesk.ticketing.domain.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.events.DomainEvent;
import dev.kaldiroglu.wolfdesk.ticketing.domain.events.MessageAdded;
import dev.kaldiroglu.wolfdesk.ticketing.domain.events.MessageRead;
import dev.kaldiroglu.wolfdesk.ticketing.domain.events.TicketAssigned;
import dev.kaldiroglu.wolfdesk.ticketing.domain.events.TicketClosed;
import dev.kaldiroglu.wolfdesk.ticketing.domain.events.TicketEscalated;
import dev.kaldiroglu.wolfdesk.ticketing.domain.events.TicketOpened;
import dev.kaldiroglu.wolfdesk.ticketing.domain.events.TicketReassigned;
import dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage.TicketClosedMessage;
import dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage.TicketEscalatedMessage;
import dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage.TicketOpenedMessage;
import dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage.TicketingMessage;

import java.util.Optional;

/**
 * Translates the ticket's domain events into ticketing's published language: the
 * open-host side of a stateless model translation (chapter 9, pp. 138 and 140). Private
 * events have no public version. The switch is exhaustive over the sealed
 * {@link DomainEvent}, so a new event does not compile until it is given a decision here.
 */
public final class PublishedLanguageTranslator {

    private PublishedLanguageTranslator() {
    }

    public static Optional<TicketingMessage> translate(DomainEvent event) {
        return switch (event) {
            case TicketOpened e -> Optional.of(new TicketOpenedMessage(e.ticketId().value(),
                    e.products().stream().map(ProductId::value).toList(), e.priority().name(), e.occurredAt()));
            case TicketEscalated e -> Optional.of(new TicketEscalatedMessage(e.ticketId().value(), e.occurredAt()));
            case TicketClosed e -> Optional.of(new TicketClosedMessage(e.ticketId().value(), e.occurredAt()));
            case TicketAssigned e -> Optional.empty();
            case MessageAdded e -> Optional.empty();
            case MessageRead e -> Optional.empty();
            case TicketReassigned e -> Optional.empty();
        };
    }
}
