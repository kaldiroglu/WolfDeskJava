/**
 * The ticket's domain model (chapter 6).
 *
 * <p>{@link dev.kaldiroglu.wolfdesk.ticketing.domain.Ticket} is an aggregate: its state
 * changes only through its commands, and each change runs in one transaction. The ticket's
 * rules on escalation and reassignment follow the book's listings 06-13 and 06-17 and the
 * ticket policies of the TypeScript {@code eventually-wolfdesk}.
 *
 * <p>{@link dev.kaldiroglu.wolfdesk.ticketing.domain.Message} shares this package with the
 * ticket on purpose: its constructor and {@code markAsRead} are package-private, so a
 * message changes only through its ticket.
 */
package dev.kaldiroglu.wolfdesk.ticketing.domain;
