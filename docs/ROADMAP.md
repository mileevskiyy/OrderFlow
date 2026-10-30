# OrderFlow roadmap

## Implemented foundation

Product catalog, warehouses, stock receipts, transactional reservations, order lifecycle, simulated payment events, fulfillment, partial returns, ledger reconciliation, session authentication, React operator interface, Flyway migrations and verification suite.

## Next milestones

1. Reservation expiry with an explicit cancellation policy and restart-safe scheduling.
2. Operator accounts, roles and persistent sessions.
3. Inventory transfers between warehouses and damaged-return disposition.
4. Payment-provider adapter with signed webhooks and transactional outbox delivery.
5. Refund lifecycle separating requested, externally confirmed and failed outcomes.
6. Search pagination, query indexes and documented performance measurements.
7. Hosted pilot, backup/restore verification and operational metrics.

Each milestone requires implementation and verification before it can be described as an existing capability in a CV or project description.
