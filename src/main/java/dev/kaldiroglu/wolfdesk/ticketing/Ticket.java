package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The ticket aggregate. Its state changes only through its commands; each command checks
 * the ticket's rules first, then changes the state and records a domain event.
 *
 * <p>Other aggregates, the customer, the agent and the products, are referenced by id only.
 * The ticket's messages belong to it: no one changes a message except through its ticket.
 */
public final class Ticket {

    private final TicketId id;
    private final int version;
    private final UserId customer;
    private final List<ProductId> products;
    private final String title;
    private final Priority priority;
    private UserId assignedAgent;
    private ResponseTimeframe responseTimeframe;
    private boolean escalated;
    private TicketStatus status;
    private final List<Message> messages;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Ticket(TicketId id, int version, UserId customer, List<ProductId> products,
                   String title, Priority priority, UserId assignedAgent,
                   ResponseTimeframe responseTimeframe, boolean escalated, TicketStatus status,
                   List<Message> messages) {
        this.id = id;
        this.version = version;
        this.customer = customer;
        this.products = products;
        this.title = title;
        this.priority = priority;
        this.assignedAgent = assignedAgent;
        this.responseTimeframe = responseTimeframe;
        this.escalated = escalated;
        this.status = status;
        this.messages = messages;
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
        var ticket = new Ticket(id, 0, customer, List.copyOf(products), title.strip(), priority,
                null, null, false, TicketStatus.OPEN, new ArrayList<>());
        ticket.domainEvents.add(new TicketOpened(id, customer, ticket.products, priority, at));
        return ticket;
    }

    public void execute(AssignAgent cmd) {
        mustBeOpen();
        assignedAgent = cmd.agent();
        responseTimeframe = cmd.timeframe();
        domainEvents.add(new TicketAssigned(id, cmd.agent(), cmd.timeframe().deadline(), cmd.at()));
    }

    public void execute(AddMessage cmd) {
        mustBeOpen();
        var agent = assignedAgentOrFail();
        UserId to;
        if (cmd.from().equals(customer)) {
            to = agent;
        } else if (cmd.from().equals(agent)) {
            to = customer;
        } else {
            throw new TicketRuleViolationException("Only the customer or the assigned agent can write");
        }
        var message = new Message(MessageId.random(), cmd.from(), to, cmd.body(), cmd.at(), false);
        messages.add(message);
        domainEvents.add(new MessageAdded(id, message.id(), cmd.from(), to, cmd.at()));
    }

    public void execute(AcknowledgeMessage cmd) {
        mustBeOpen();
        var message = messages.stream()
                .filter(x -> x.id().equals(cmd.messageId()))
                .findFirst()
                .orElseThrow(() -> new TicketRuleViolationException("No such message"));
        if (!message.to().equals(cmd.by())) {
            throw new TicketRuleViolationException("Only the receiver can acknowledge a message");
        }
        if (message.wasRead()) return;
        message.markAsRead();
        domainEvents.add(new MessageRead(id, message.id(), cmd.at()));
    }

    public void execute(RequestEscalation cmd) {
        mustBeOpen();
        if (!cmd.requestedBy().equals(customer)) {
            throw new TicketRuleViolationException("Only the customer can request an escalation");
        }
        if (escalated) {
            throw new TicketRuleViolationException("The ticket is already escalated");
        }
        if (getRemainingTimePercentage(cmd.at()) > 0) {
            throw new TicketRuleViolationException("The response deadline has not passed yet");
        }
        escalated = true;
        domainEvents.add(new TicketEscalated(id, EscalationReason.CUSTOMER_REQUEST, cmd.at()));
    }

    public void execute(EvaluateAutomaticActions cmd) {
        if (status != TicketStatus.OPEN || assignedAgent == null) return;
        var remaining = getRemainingTimePercentage(cmd.at());

        if (!escalated && remaining <= 0) {
            escalated = true;
            domainEvents.add(new TicketEscalated(id, EscalationReason.MISSED_SLA, cmd.at()));
        }

        if (escalated && remaining < 0.5 && getUnreadMessagesCount(assignedAgent) > 0) {
            var previous = assignedAgent;
            assignedAgent = cmd.replacementAgent();
            domainEvents.add(new TicketReassigned(id, previous, assignedAgent, cmd.at()));
        }
    }

    public void execute(CloseTicket cmd) {
        mustBeOpen();
        if (!cmd.closedBy().equals(customer) && !cmd.closedBy().equals(assignedAgent)) {
            throw new TicketRuleViolationException("Only the customer or the assigned agent can close");
        }
        status = TicketStatus.CLOSED;
        domainEvents.add(new TicketClosed(id, cmd.closedBy(), cmd.at()));
    }

    public int getUnreadMessagesCount(UserId id) {
        return (int) messages.stream().filter(x -> x.to().equals(id) && !x.wasRead()).count();
    }

    public double getRemainingTimePercentage(Instant now) {
        if (responseTimeframe == null) return 1.0;
        return responseTimeframe.remainingTimePercentage(now);
    }

    /** The events recorded since the ticket was loaded; returns them once. */
    public List<DomainEvent> pullDomainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void mustBeOpen() {
        if (status != TicketStatus.OPEN) throw new TicketRuleViolationException("The ticket is closed");
    }

    private UserId assignedAgentOrFail() {
        if (assignedAgent == null) throw new TicketRuleViolationException("No agent is assigned yet");
        return assignedAgent;
    }

    public TicketId id() {
        return id;
    }

    public int version() {
        return version;
    }

    public UserId customer() {
        return customer;
    }

    public List<ProductId> products() {
        return products;
    }

    public String title() {
        return title;
    }

    public Priority priority() {
        return priority;
    }

    public Optional<UserId> assignedAgent() {
        return Optional.ofNullable(assignedAgent);
    }

    public boolean isEscalated() {
        return escalated;
    }

    public TicketStatus status() {
        return status;
    }

    public List<Message> messages() {
        return Collections.unmodifiableList(messages);
    }

    public TicketSnapshot snapshot() {
        return new TicketSnapshot(id, version, customer, products, title, priority, assignedAgent,
                responseTimeframe, escalated, status, messages.stream()
                .map(m -> new TicketSnapshot.MessageSnapshot(m.id(), m.from(), m.to(), m.body(),
                        m.sentAt(), m.wasRead()))
                .toList());
    }

    public static Ticket restore(TicketSnapshot s) {
        var messages = new ArrayList<Message>();
        s.messages().forEach(m -> messages.add(
                new Message(m.id(), m.from(), m.to(), m.body(), m.sentAt(), m.wasRead())));
        return new Ticket(s.id(), s.version(), s.customer(), s.products(), s.title(), s.priority(),
                s.assignedAgent(), s.responseTimeframe(), s.escalated(), s.status(), messages);
    }
}
