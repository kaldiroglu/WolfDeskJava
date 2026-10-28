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
