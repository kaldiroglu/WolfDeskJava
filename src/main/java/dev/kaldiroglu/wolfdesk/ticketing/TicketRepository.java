package dev.kaldiroglu.wolfdesk.ticketing;

/**
 * Loads and saves tickets, one per transaction. {@link #save} uses optimistic concurrency:
 * it fails with {@link ConcurrencyException} if the ticket changed since it was loaded
 * (listings 06-11 and 06-12). Load a ticket again before each change.
 */
public interface TicketRepository {

    Ticket load(TicketId id);

    void save(Ticket ticket);
}
