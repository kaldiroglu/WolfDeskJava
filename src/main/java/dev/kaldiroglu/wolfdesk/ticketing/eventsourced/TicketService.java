package dev.kaldiroglu.wolfdesk.ticketing.eventsourced;

import dev.kaldiroglu.wolfdesk.ticketing.AcknowledgeMessage;
import dev.kaldiroglu.wolfdesk.ticketing.AddMessage;
import dev.kaldiroglu.wolfdesk.ticketing.AssignAgent;
import dev.kaldiroglu.wolfdesk.ticketing.CloseTicket;
import dev.kaldiroglu.wolfdesk.ticketing.ConcurrencyException;
import dev.kaldiroglu.wolfdesk.ticketing.EvaluateAutomaticActions;
import dev.kaldiroglu.wolfdesk.ticketing.ExecutionResult;
import dev.kaldiroglu.wolfdesk.ticketing.MessageId;
import dev.kaldiroglu.wolfdesk.ticketing.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.RequestEscalation;
import dev.kaldiroglu.wolfdesk.ticketing.ResponseTimeframeCalculationService;
import dev.kaldiroglu.wolfdesk.ticketing.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.TicketNotFoundException;
import dev.kaldiroglu.wolfdesk.ticketing.TicketRuleViolationException;
import dev.kaldiroglu.wolfdesk.ticketing.UserId;

import java.time.Clock;
import java.util.List;

/**
 * The application service of the event-sourced ticket. Each operation follows the book's
 * script (p. 108): load the ticket's events, rehydrate the ticket, execute one command, and
 * append the new events at the version the ticket was loaded at.
 */
public final class TicketService {

    private final EventStore eventStore;
    private final ResponseTimeframeCalculationService timeframes;
    private final Clock clock;

    public TicketService(EventStore eventStore, ResponseTimeframeCalculationService timeframes, Clock clock) {
        this.eventStore = eventStore;
        this.timeframes = timeframes;
        this.clock = clock;
    }

    public TicketId open(UserId customer, List<ProductId> products, String title, Priority priority) {
        var ticket = Ticket.open(TicketId.random(), customer, products, title, priority, clock.instant());
        eventStore.append(ticket.id(), ticket.newEvents(), -1);
        return ticket.id();
    }

    public void assign(TicketId id, UserId agent) {
        var ticket = load(id);
        var originalVersion = ticket.version();
        var now = clock.instant();
        var timeframe = timeframes.calculateAgentResponseDeadline(agent, ticket.priority(),
                ticket.isEscalated(), now);
        ticket.execute(new AssignAgent(agent, timeframe, now));
        eventStore.append(id, ticket.newEvents(), originalVersion);
    }

    public MessageId addMessage(TicketId id, UserId from, String body) {
        var ticket = load(id);
        var originalVersion = ticket.version();
        ticket.execute(new AddMessage(from, body, clock.instant()));
        eventStore.append(id, ticket.newEvents(), originalVersion);
        return ticket.messages().getLast().id();
    }

    public void acknowledge(TicketId id, MessageId messageId, UserId by) {
        var ticket = load(id);
        var originalVersion = ticket.version();
        ticket.execute(new AcknowledgeMessage(messageId, by, clock.instant()));
        eventStore.append(id, ticket.newEvents(), originalVersion);
    }

    /** The book's {@code TicketAPI.RequestEscalation}, p. 108; failures are reported, not thrown. */
    public ExecutionResult escalate(TicketId id, UserId requestedBy) {
        try {
            var events = eventStore.fetch(id);
            var ticket = new Ticket(events);
            var originalVersion = ticket.version();
            var cmd = new RequestEscalation(requestedBy, clock.instant());
            ticket.execute(cmd);
            eventStore.append(id, ticket.newEvents(), originalVersion);
            return ExecutionResult.success();
        } catch (ConcurrencyException | TicketRuleViolationException ex) {
            return ExecutionResult.failure(ex);
        }
    }

    public void evaluateAutomaticActions(TicketId id, UserId replacementAgent) {
        var ticket = load(id);
        var originalVersion = ticket.version();
        ticket.execute(new EvaluateAutomaticActions(clock.instant(), replacementAgent));
        eventStore.append(id, ticket.newEvents(), originalVersion);
    }

    public void close(TicketId id, UserId closedBy) {
        var ticket = load(id);
        var originalVersion = ticket.version();
        ticket.execute(new CloseTicket(closedBy, clock.instant()));
        eventStore.append(id, ticket.newEvents(), originalVersion);
    }

    public Ticket load(TicketId id) {
        var events = eventStore.fetch(id);
        if (events.isEmpty()) throw new TicketNotFoundException(id);
        return new Ticket(events);
    }

    /** Time travel: the ticket as it was at {@code version}, rebuilt from its first events only. */
    public Ticket loadAt(TicketId id, int version) {
        var events = eventStore.fetch(id);
        if (version < 0 || version >= events.size()) {
            throw new IllegalArgumentException("Ticket " + id.value() + " has no version " + version);
        }
        return new Ticket(events.subList(0, version + 1));
    }
}
