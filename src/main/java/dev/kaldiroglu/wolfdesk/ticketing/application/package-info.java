/**
 * Ticketing's application layer: {@code TicketService}, what its operations report back,
 * and {@code OutboxRelay}, which publishes the outbox's events in the published language
 * (chapter 9). Its one port, {@code EventPublisher}, carries that published language, so it
 * stays here rather than in the domain.
 */
package dev.kaldiroglu.wolfdesk.ticketing.application;
