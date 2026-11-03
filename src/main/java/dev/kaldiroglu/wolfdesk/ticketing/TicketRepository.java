package dev.kaldiroglu.wolfdesk.ticketing;

/**
 * Loads and saves tickets, one per transaction. {@link #save} uses optimistic concurrency:
 * it fails with {@link ConcurrencyException} if the ticket changed since it was loaded
 * (listings 06-11 and 06-12). Load a ticket again before each change.
 *
 * <p>{@link #save} also writes the ticket's new domain events to the {@link Outbox}, in the
 * same transaction: both are saved, or neither is (chapter 9, p. 145).
 */
public interface TicketRepository {

    Ticket load(TicketId id);

    void save(Ticket ticket);
}
