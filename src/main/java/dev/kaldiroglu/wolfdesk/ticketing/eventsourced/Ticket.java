package dev.kaldiroglu.wolfdesk.ticketing.eventsourced;

import dev.kaldiroglu.wolfdesk.ticketing.AcknowledgeMessage;
import dev.kaldiroglu.wolfdesk.ticketing.AddMessage;
import dev.kaldiroglu.wolfdesk.ticketing.AssignAgent;
import dev.kaldiroglu.wolfdesk.ticketing.CloseTicket;
import dev.kaldiroglu.wolfdesk.ticketing.EscalationReason;
import dev.kaldiroglu.wolfdesk.ticketing.EvaluateAutomaticActions;
import dev.kaldiroglu.wolfdesk.ticketing.MessageId;
import dev.kaldiroglu.wolfdesk.ticketing.Priority;
import dev.kaldiroglu.wolfdesk.ticketing.ProductId;
import dev.kaldiroglu.wolfdesk.ticketing.RequestEscalation;
import dev.kaldiroglu.wolfdesk.ticketing.TicketId;
import dev.kaldiroglu.wolfdesk.ticketing.TicketRuleViolationException;
import dev.kaldiroglu.wolfdesk.ticketing.TicketStatus;
import dev.kaldiroglu.wolfdesk.ticketing.UserId;
import dev.kaldiroglu.wolfdesk.ticketing.eventsourced.TicketEvent.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The ticket aggregate, event-sourced. It has chapter 6's commands and rules, but no stored
 * state: the constructor rebuilds the state from the ticket's events, and a command that
 * passes the rules appends a new event instead of changing a field.
 */
public final class Ticket {

    private final TicketState state = new TicketState();
    private final List<TicketEvent> newEvents = new ArrayList<>();

    /** Rehydrates the ticket: projects its events, oldest first, into its current state. */
    public Ticket(List<TicketEvent> events) {
        for (var event : events) {
            state.apply(event);
        }
    }

    public static Ticket open(TicketId id, UserId customer, List<ProductId> products,
                              String title, Priority priority, Instant at) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(customer, "customer");
        Objects.requireNonNull(priority, "priority");
        if (products == null || products.isEmpty()) {
            throw new TicketRuleViolationException("A ticket is about at least one product");
        }
        if (title == null || title.isBlank()) {
            throw new TicketRuleViolationException("A ticket needs a title");
        }
        var ticket = new Ticket(List.of());
        ticket.appendEvent(new TicketOpened(id, customer, List.copyOf(products), title.strip(), priority, at));
        return ticket;
    }

    public void execute(AssignAgent cmd) {
        mustBeOpen();
        appendEvent(new TicketAssigned(state.id(), cmd.agent(), cmd.timeframe(), cmd.at()));
    }

    public void execute(AddMessage cmd) {
        mustBeOpen();
        var agent = assignedAgentOrFail();
        UserId to;
        if (cmd.from().equals(state.customer())) {
            to = agent;
        } else if (cmd.from().equals(agent)) {
            to = state.customer();
        } else {
            throw new TicketRuleViolationException("Only the customer or the assigned agent can write");
        }
        if (cmd.body() == null || cmd.body().isBlank()) {
            throw new IllegalArgumentException("A message needs a body");
        }
        appendEvent(new MessageAdded(state.id(), MessageId.random(), cmd.from(), to, cmd.body(), cmd.at()));
    }

    public void execute(AcknowledgeMessage cmd) {
        mustBeOpen();
        var message = state.messages().stream()
                .filter(x -> x.id().equals(cmd.messageId()))
                .findFirst()
                .orElseThrow(() -> new TicketRuleViolationException("No such message"));
        if (!message.to().equals(cmd.by())) {
            throw new TicketRuleViolationException("Only the receiver can acknowledge a message");
        }
        if (message.wasRead()) return;
        appendEvent(new MessageRead(state.id(), message.id(), cmd.at()));
    }

    public void execute(RequestEscalation cmd) {
        mustBeOpen();
        if (!cmd.requestedBy().equals(state.customer())) {
            throw new TicketRuleViolationException("Only the customer can request an escalation");
        }
        if (state.isEscalated()) {
            throw new TicketRuleViolationException("The ticket is already escalated");
        }
        if (getRemainingTimePercentage(cmd.at()) > 0) {
            throw new TicketRuleViolationException("The response deadline has not passed yet");
        }
        appendEvent(new TicketEscalated(state.id(), EscalationReason.CUSTOMER_REQUEST, cmd.at()));
    }

    public void execute(EvaluateAutomaticActions cmd) {
        if (state.status() != TicketStatus.OPEN || state.assignedAgent().isEmpty()) return;
        var remaining = getRemainingTimePercentage(cmd.at());

        if (!state.isEscalated() && remaining <= 0) {
            appendEvent(new TicketEscalated(state.id(), EscalationReason.MISSED_SLA, cmd.at()));
        }

        var agent = state.assignedAgent().get();
        if (state.isEscalated() && remaining < 0.5 && getUnreadMessagesCount(agent) > 0) {
            appendEvent(new TicketReassigned(state.id(), agent, cmd.replacementAgent(), cmd.at()));
        }
    }

    public void execute(CloseTicket cmd) {
        mustBeOpen();
        var closedBy = cmd.closedBy();
        if (!closedBy.equals(state.customer()) && !state.assignedAgent().equals(Optional.of(closedBy))) {
            throw new TicketRuleViolationException("Only the customer or the assigned agent can close");
        }
        appendEvent(new TicketClosed(state.id(), closedBy, cmd.at()));
    }

    public int getUnreadMessagesCount(UserId id) {
        return (int) state.messages().stream().filter(x -> x.to().equals(id) && !x.wasRead()).count();
    }

    public double getRemainingTimePercentage(Instant now) {
        return state.responseTimeframe().map(t -> t.remainingTimePercentage(now)).orElse(1.0);
    }

    /** The events appended since the ticket was rehydrated, for the event store. */
    public List<TicketEvent> newEvents() {
        return List.copyOf(newEvents);
    }

    /** A new event changes the state through the projection, the same way a loaded one does. */
    private void appendEvent(TicketEvent event) {
        state.apply(event);
        newEvents.add(event);
    }

    private void mustBeOpen() {
        if (state.status() != TicketStatus.OPEN) throw new TicketRuleViolationException("The ticket is closed");
    }

    private UserId assignedAgentOrFail() {
        return state.assignedAgent()
                .orElseThrow(() -> new TicketRuleViolationException("No agent is assigned yet"));
    }

    public TicketId id() {
        return state.id();
    }

    /** The number of the ticket's last event: 0 once opened, -1 for no events. */
    public int version() {
        return state.version();
    }

    public UserId customer() {
        return state.customer();
    }

    public String title() {
        return state.title();
    }

    public Priority priority() {
        return state.priority();
    }

    public Optional<UserId> assignedAgent() {
        return state.assignedAgent();
    }

    public boolean isEscalated() {
        return state.isEscalated();
    }

    public TicketStatus status() {
        return state.status();
    }

    public List<TicketState.Message> messages() {
        return state.messages();
    }
}
