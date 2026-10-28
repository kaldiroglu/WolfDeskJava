**Claude Opus 5.5 · October 28, 2026**

# Tests

All tests are unit tests (JUnit 5), under `src/test/java`, run with `mvn test`. They use
the in-memory adapters, so they need no database. There are no integration or end-to-end
tests yet; those come with PostgreSQL in chapter 8.

## billing (transaction script)

- `VolumeDiscountTest` — the Preface's thresholds: 0% up to 500 tickets, 10% from 501,
  20% from 751, 30% from 1,001.
- `ChargeTenantTest` — the invoice counts only the period's tickets and applies the
  discount; charging a period twice returns the first invoice; a failure after the
  invoice is written rolls the invoice back.
- `RecordTicketOpenedTest` — a retried call counts the ticket once (idempotency).

## tenants (active record)

- `TenantTest` — a tenant is saved and found with its categories and products; a blank
  name is rejected; a failed script rolls back; rolled-back changes are not kept; a deleted
  tenant is gone.

## ticketing (domain model)

- `ValueObjectTest` — ids are equal by value and never empty; a response timeframe ends
  after it starts and reports the share of time left.
- `TicketTest` — the aggregate's rules: who may write and acknowledge, when an escalation
  may be requested, automatic escalation on a missed deadline, reassignment of an
  escalated ticket with unread messages and less than half its time left, closing, and a
  restored ticket keeping its state.
- `ResponseTimeframeCalculationServiceTest` — the domain service counts working time in
  shifts only, applies the escalation factor, and fails when the shifts do not cover the
  response time.
- `InMemoryTicketRepositoryTest` — each save increments the version; a ticket changed since
  it was loaded is not saved.
- `TicketServiceTest` — events are published after the save; the agent's deadline comes
  from the domain service; a concurrent change and a rejected escalation are reported as
  failures, not thrown.

The concurrency tests were checked against a deliberately broken repository (version check
removed): two of them failed, as they should.
