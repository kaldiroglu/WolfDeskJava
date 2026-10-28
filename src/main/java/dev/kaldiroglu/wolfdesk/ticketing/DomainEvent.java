package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;

/** Something significant that has happened to a ticket. Named in the past tense. */
public sealed interface DomainEvent
        permits TicketOpened, TicketAssigned, MessageAdded, MessageRead, TicketEscalated,
                TicketReassigned, TicketClosed {

    TicketId ticketId();

    Instant occurredAt();
}
