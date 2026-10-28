/**
 * Read models of ticketing: the CQRS pattern (chapter 8).
 *
 * <p>The {@code Ticket} aggregate is the command execution model, the only strongly
 * consistent one. The read models here are projections of it: they are rebuilt from the
 * saved tickets, never changed by a command, and can be wiped and projected again.
 */
package dev.kaldiroglu.wolfdesk.ticketing.readmodels;
