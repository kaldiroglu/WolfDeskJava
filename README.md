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
- Tags by chapter (`stage-ch05`, `stage-ch06`, …), so a slide can point to the code as it
  stood at that chapter.

## Bounded contexts and their patterns

| Package | Pattern | Chapter | What it does |
|---|---|---|---|
| `billing` | Transaction script | 5 | Records opened tickets and charges a tenant per period with the volume discount |
| `tenants` | Active record | 5 | A tenant with its ticket categories and supported products |
| `ticketing` | Domain model | 6 | The `Ticket` aggregate: messages, assignment, escalation, reassignment, closing |

Each context has its own small model; they share no classes. Storage is in-memory
adapters that mimic a database: a transaction works on a copy of the tables, which commit
keeps and rollback throws away. PostgreSQL comes with chapter 8.

## Run it with

```bash
mvn test                  # all tests
mvn -Dtest=ChargeTenantTest test   # one test class
```
