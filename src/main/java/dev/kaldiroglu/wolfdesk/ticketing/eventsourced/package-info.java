/**
 * The ticket aggregate of chapter 6 as an event-sourced domain model (chapter 7).
 *
 * <p>The commands and the rules are the same as in {@code ticketing}; the difference is the
 * way the ticket's state is persisted. Nothing stores the state: the {@link
 * dev.kaldiroglu.wolfdesk.ticketing.eventsourced.EventStore} keeps the ticket's events, and
 * {@link dev.kaldiroglu.wolfdesk.ticketing.eventsourced.Ticket} rebuilds its state from them
 * through {@link dev.kaldiroglu.wolfdesk.ticketing.eventsourced.TicketState}, the book's
 * listing on pp. 108–110. The same events can be projected into other models, such as
 * {@link dev.kaldiroglu.wolfdesk.ticketing.eventsourced.TicketAnalysisProjection}.
 */
package dev.kaldiroglu.wolfdesk.ticketing.eventsourced;
