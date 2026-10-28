/**
 * Ticketing: the life of a support ticket, from opening to closing.
 *
 * <p>Implemented with the domain model pattern (chapter 6). {@link Ticket} is an aggregate:
 * its state changes only through its commands, and each change runs in one transaction.
 * The ticket's rules on escalation and reassignment follow the book's listings 06-13 and
 * 06-17 and the ticket policies of the TypeScript {@code eventually-wolfdesk}.
 */
package dev.kaldiroglu.wolfdesk.ticketing;
