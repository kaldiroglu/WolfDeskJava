**Claude Opus 5.5 · October 28, 2026**

# WolfDesk (Java)

For further enquiry please contact Akin Kaldiroglu at akin@kaldiroglu.dev

WolfDesk is the running example of the Domain-Driven Design course, based on Vlad
Khononov's *Learning Domain-Driven Design* (O'Reilly, 2021). The course decks quote this
code, so every snippet on a slide is compiled and tested. The C# version is
`WolfDeskCSharp`.

- **Project:** WolfDesk, Java version, started on 2026-10-28.
- **Stack:** Java 25, Maven, JUnit 5. No framework in the domain.
- **Root package:** `dev.kaldiroglu.wolfdesk`.

## The business

WolfDesk provides a help desk ticket management system as a service (the book's Preface,
pp. xviii–xix). Tenants pay for the number of tickets opened per charging period, with
automatic volume discounts of 10% above 500 tickets a month, 20% above 750 and 30% above
1,000. Tenants configure their ticket categories and the products they support. Tickets are
assigned to support agents during their shifts, and escalated when the response deadline
passes.

## Expected benefits

- One code base that shows each of the book's business logic and architectural patterns in
  the bounded context where the book would use it.
- The same design in Java and C#, so participants work in their own language.
- Tags by chapter (`stage-ch05` to `stage-ch08`, …), so a slide can point to
  the code as it stood at that chapter.

## Bounded contexts and their patterns

| Package | Pattern | Chapter | What it does |
|---|---|---|---|
| `billing` | Transaction script | 5 | Records opened tickets and charges a tenant per period with the volume discount |
| `tenants` | Active record | 5 | A tenant with its ticket categories and supported products |
| `ticketing` | Domain model | 6 | The `Ticket` aggregate: messages, assignment, escalation, reassignment, closing |
| `ticketing.eventsourced` | Event-sourced domain model | 7 | The same ticket, its state rebuilt from its events |
| `ticketing.readmodels` | CQRS | 8 | The agents' queues, projected from the saved tickets |

Each context has its own small model; they share no classes. Storage is in-memory
adapters that mimic a database: a transaction works on a copy of the tables, which commit
keeps and rollback throws away.

## Architecture

Each module has the architecture its business logic needs (chapter 8):

- `tenants` is layered: the `Tenant` active record depends on the concrete
  `TenantsDatabase`, and the transaction scripts `RegisterTenant` and `AddSupportedProduct`
  are its service layer.
- `billing` is transaction scripts, which are a service layer already; they work on the
  database through the `BillingDatabase` interface.
- `ticketing` is ports & adapters. The domain model and the application service
  (`TicketService`) define the ports they need — `TicketRepository`, `EventPublisher`,
  `DepartmentRepository` — and `ticketing.inmemory` holds the adapters. Nothing in
  `ticketing` imports an adapter.
- `ticketing.eventsourced` is ports & adapters too: `EventStore` is its port, and
  `InMemoryEventStore` in `ticketing.inmemory` its adapter.
- `ticketing.readmodels` adds CQRS: the `Ticket` aggregate is the command execution model,
  and `AgentQueueProjection` projects the saved tickets into each agent's queue. Each
  catch-up reads the tickets saved after its last checkpoint, which the repository stamps on
  every save, like a `rowversion` column. Resetting the checkpoint rebuilds the queues.

## The ticket's rules

The `Ticket` aggregate follows the book's listings 06-08 to 06-17 for its shape, and the
ticket policies of the TypeScript `eventually-wolfdesk` for its rules:

- Only the customer or the assigned agent writes messages; only a message's receiver can
  acknowledge it.
- The customer can request an escalation only after the response deadline, and only once.
- A missed deadline escalates the ticket automatically.
- An escalated ticket with less than half its time left and unread messages for its agent
  goes to another agent (listing 06-13).
- The response deadline counts the agent's working time only, from the shift schedule
  (listing 06-18, a domain service).
- Saving a ticket that changed since it was loaded fails (optimistic concurrency, listings
  06-10 to 06-12).

## The event-sourced ticket

`ticketing.eventsourced` implements the same `Ticket` aggregate as an event-sourced domain
model (chapter 7, pp. 108–110). It has the same commands and rules; only the way its state
is persisted differs:

- No state is stored. `EventStore` keeps each ticket's events, append-only: it can fetch a
  ticket's events and append new ones, nothing else.
- `new Ticket(events)` rehydrates the ticket: `TicketState` projects the events, oldest
  first, into the current state. A command that passes the rules appends a new event, and
  the event changes the state through the same projection.
- `TicketService` follows the book's script: load the events, rehydrate, execute one
  command, append the new events at the version the ticket was loaded at. If other events
  were appended since, the append fails (optimistic concurrency).
- The events are the source of truth, so they carry everything: the title, the response
  timeframe, the message body. Chapter 6's events do not; they were never meant to rebuild
  a ticket.
- `loadAt(id, version)` rebuilds a ticket as it was at an earlier version, and
  `TicketAnalysisProjection` projects the same events into a second model for analysis.

## Credits

The ticket's rules follow `wolfdesk/eventually-wolfdesk`, MIT License, Copyright (c) 2021
Vladik Khononov; its license is in `THIRD-PARTY-LICENSE-eventually-wolfdesk`. The code here
is written from scratch in Java; nothing is translated line by line.

## Run it with

```bash
mvn test                  # all tests
mvn -Dtest=ChargeTenantTest test   # one test class
```
