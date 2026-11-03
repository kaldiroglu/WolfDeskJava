/**
 * Ticketing's application layer: {@code TicketService}, what its operations report back,
 * and the ports it needs, the repository, the outbox and the event publisher, which
 * {@code ticketing.inmemory} implements. {@code OutboxRelay} publishes the outbox's events
 * in the published language (chapter 9).
 */
package dev.kaldiroglu.wolfdesk.ticketing.application;
