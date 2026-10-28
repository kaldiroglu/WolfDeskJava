package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Clock;
import java.util.List;

/**
 * The application service: each operation loads one ticket, executes one command on it,
 * saves it, and then publishes its domain events. It holds no business rules itself.
 */
public final class TicketService {

    private final TicketRepository ticketRepository;
    private final ResponseTimeframeCalculationService timeframes;
    private final EventPublisher publisher;
    private final Clock clock;

    public TicketService(TicketRepository ticketRepository,
                         ResponseTimeframeCalculationService timeframes,
                         EventPublisher publisher, Clock clock) {
        this.ticketRepository = ticketRepository;
        this.timeframes = timeframes;
        this.publisher = publisher;
        this.clock = clock;
    }

    public TicketId open(UserId customer, List<ProductId> products, String title, Priority priority) {
        var ticket = Ticket.open(TicketId.random(), customer, products, title, priority, clock.instant());
        save(ticket);
        return ticket.id();
    }

    public void assign(TicketId id, UserId agent) {
        var ticket = ticketRepository.load(id);
        var now = clock.instant();
        var timeframe = timeframes.calculateAgentResponseDeadline(agent, ticket.priority(),
                ticket.isEscalated(), now);
        ticket.execute(new AssignAgent(agent, timeframe, now));
        save(ticket);
    }

    public MessageId addMessage(TicketId id, UserId from, String body) {
        var ticket = ticketRepository.load(id);
        ticket.execute(new AddMessage(from, body, clock.instant()));
        save(ticket);
        return ticket.messages().getLast().id();
    }

    public void acknowledge(TicketId id, MessageId messageId, UserId by) {
        var ticket = ticketRepository.load(id);
        ticket.execute(new AcknowledgeMessage(messageId, by, clock.instant()));
        save(ticket);
    }

    /** Listing 06-10: a concurrent change is reported back, not thrown. */
    public ExecutionResult escalate(TicketId id, UserId requestedBy) {
        try {
            var ticket = ticketRepository.load(id);
            var cmd = new RequestEscalation(requestedBy, clock.instant());
            ticket.execute(cmd);
            save(ticket);
            return ExecutionResult.success();
        } catch (ConcurrencyException | TicketRuleViolationException ex) {
            return ExecutionResult.failure(ex);
        }
    }

    public void evaluateAutomaticActions(TicketId id, UserId replacementAgent) {
        var ticket = ticketRepository.load(id);
        ticket.execute(new EvaluateAutomaticActions(clock.instant(), replacementAgent));
        save(ticket);
    }

    public void close(TicketId id, UserId closedBy) {
        var ticket = ticketRepository.load(id);
        ticket.execute(new CloseTicket(closedBy, clock.instant()));
        save(ticket);
    }

    /** The events are published after the save; chapter 9's outbox makes the pair reliable. */
    private void save(Ticket ticket) {
        ticketRepository.save(ticket);
        ticket.pullDomainEvents().forEach(publisher::publish);
    }
}
