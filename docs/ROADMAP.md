# OrderFlow roadmap

## Implemented foundation

Product catalog, warehouses, stock receipts, transactional reservations, order lifecycle, simulated payment events, fulfillment, partial returns, ledger reconciliation, session authentication, React operator interface, Flyway migrations and verification suite.

## Implemented: connected QA evidence

Actual OrderFlow browser workflow delivery to QualityHub, pinned reporter/target, source context, controlled-failure/recovery comparison, attachment hash verification and replay checks. CI exports the experiment evidence. This is separate from the planned business-domain changes below.

## Implemented: reservation deadlines and recovery

Persisted UTC deadline per order; bounded automatic scans and startup recovery; transactional, concurrency-safe stock release; deadline enforcement during payment; explicit late-success review effects and filtering; expiry UI/countdown; V1 migration policy and restart/race/rollback/browser verification. Recovery requires persistent storage; the public demo remains disposable.

## Next milestones

1. More advanced reservation policies after measuring real operational needs (extensions, admission limits and expiry backlog monitoring).
2. Operator accounts, roles and persistent sessions.
3. Inventory transfers between warehouses and damaged-return disposition.
4. Payment-provider adapter with signed webhooks and transactional outbox delivery.
5. Refund lifecycle separating requested, externally confirmed and failed outcomes.
6. Search pagination, query indexes and documented performance measurements.
7. Hosted pilot, backup/restore verification and operational metrics.

Each milestone requires implementation and verification before it can be described as an existing capability in a CV or project description.
