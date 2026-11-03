package dev.kaldiroglu.wolfdesk.ticketing.application;

import dev.kaldiroglu.wolfdesk.ticketing.domain.MessageId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.domain.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.ResponseTimeframeCalculationService;
import dev.kaldiroglu.wolfdesk.ticketing.domain.Ticket;
import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.TicketRuleViolationException;
import dev.kaldiroglu.wolfdesk.ticketing.domain.UserId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.commands.AcknowledgeMessage;
import dev.kaldiroglu.wolfdesk.ticketing.domain.commands.AddMessage;
import dev.kaldiroglu.wolfdesk.ticketing.domain.commands.AssignAgent;
import dev.kaldiroglu.wolfdesk.ticketing.domain.commands.CloseTicket;
import dev.kaldiroglu.wolfdesk.ticketing.domain.commands.EvaluateAutomaticActions;
import dev.kaldiroglu.wolfdesk.ticketing.domain.commands.RequestEscalation;

import java.time.Clock;
import java.util.List;

/**
 * The application service: each operation loads one ticket, executes one command on it,
 * and saves it. It holds no business rules itself. It publishes nothing: the repository
 * keeps the ticket's events in the outbox, and {@link OutboxRelay} publishes them.
 */
public final class TicketService {

    private final TicketRepository ticketRepository;
    private final ResponseTimeframeCalculationService timeframes;
    private final Clock clock;

    public TicketService(TicketRepository ticketRepository,
                         ResponseTimeframeCalculationService timeframes, Clock clock) {
        this.ticketRepository = ticketRepository;
        this.timeframes = timeframes;
        this.clock = clock;
    }

    public TicketId open(UserId customer, List<ProductId> products, String title, Priority priority) {
        var ticket = Ticket.open(TicketId.random(), customer, products, title, priority, clock.instant());
        ticketRepository.save(ticket);
        return ticket.id();
    }

    public void assign(TicketId id, UserId agent) {
        var ticket = ticketRepository.load(id);
        var now = clock.instant();
        var timeframe = timeframes.calculateAgentResponseDeadline(agent, ticket.priority(),
                ticket.isEscalated(), now);
        ticket.execute(new AssignAgent(agent, timeframe, now));
        ticketRepository.save(ticket);
    }

    public MessageId addMessage(TicketId id, UserId from, String body) {
        var ticket = ticketRepository.load(id);
        ticket.execute(new AddMessage(from, body, clock.instant()));
        ticketRepository.save(ticket);
        return ticket.messages().getLast().id();
    }

    public void acknowledge(TicketId id, MessageId messageId, UserId by) {
        var ticket = ticketRepository.load(id);
        ticket.execute(new AcknowledgeMessage(messageId, by, clock.instant()));
        ticketRepository.save(ticket);
    }

    /** Listing 06-10: a concurrent change is reported back, not thrown. */
    public ExecutionResult escalate(TicketId id, UserId requestedBy) {
        try {
            var ticket = ticketRepository.load(id);
            var cmd = new RequestEscalation(requestedBy, clock.instant());
            ticket.execute(cmd);
            ticketRepository.save(ticket);
            return ExecutionResult.success();
        } catch (ConcurrencyException | TicketRuleViolationException ex) {
            return ExecutionResult.failure(ex);
        }
    }

    public void evaluateAutomaticActions(TicketId id, UserId replacementAgent) {
        var ticket = ticketRepository.load(id);
        ticket.execute(new EvaluateAutomaticActions(clock.instant(), replacementAgent));
        ticketRepository.save(ticket);
    }

    public void close(TicketId id, UserId closedBy) {
        var ticket = ticketRepository.load(id);
        ticket.execute(new CloseTicket(closedBy, clock.instant()));
        ticketRepository.save(ticket);
    }
}
