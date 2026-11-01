# Architecture notes

## A modular application with explicit domain transactions

The first release uses one Spring Boot process and one relational database. This keeps reservation, fulfillment and return changes inside a single transaction boundary. Introducing separate inventory/payment services without a real operational requirement would add distributed consistency problems without improving the current product.

HTTP request records use Jakarta Bean Validation. The controller delegates to a Spring-managed service, whose mutation methods are transactional. JDBC statements are parameterized. A Flyway migration defines foreign keys, uniqueness rules, nonnegative balances and line-level return constraints.

## Lock ordering

Idempotent operations acquire their operation-key bucket before other resources. Order mutations lock the order before changing its lines or inventory. Inventory positions are locked in product-identifier order. Catalog/warehouse creation shares a catalog lock so a product and warehouse created simultaneously cannot miss their corresponding inventory position.

The 64 lock-bucket rows live in the database. Their purpose is to coordinate the first arrival of the same idempotency key across application processes. Bucket collisions reduce concurrency for unrelated requests; performance has not been benchmarked.

## Request identity

The fingerprint for an order includes the customer, warehouse and sorted product quantities. Stock-receipt identity includes warehouse, product, quantity and reason. Return identity is scoped to the order and includes the sorted return lines and reason. Fingerprints are SHA-256 hashes of these canonical inputs.

Replays return the existing resource's current representation. They guarantee no duplicate business effect; they do not promise a byte-identical historic HTTP response.

## Payment simulation

Simulator events include an event ID, order ID, amount and outcome. The amount must match the snapshotted order total. Reused IDs must match the previous event payload. Outcomes are recorded with their applied or ignored effect. They are authenticated operator actions, not publicly accepted payment-provider callbacks.

A failure leaves a reservation pending. A confirmed order ignores later failure notifications. Cancelled orders do not become paid when an old success notification arrives. No real funds are moved by any of these transitions.

## Returns

A return is accepted only after fulfillment. Each requested line must belong to the order and its quantity cannot exceed the remaining unreturned balance. Refund calculation uses original unit-price snapshots. Returned stock, line quantities, refund totals, state, activity and idempotency records commit together.

The current policy restocks every returned unit. A later damaged-stock workflow needs a separate disposition/quarantine model and must not be implied by the current implementation.

## Security

Spring Security authenticates the configured operator, hashes the configured password and manages the session. Interactive mutations require a session CSRF token retrieved through `/api/session`. Private API paths return 401 without authentication and reject missing CSRF tokens. Production configuration must supply private credentials and HTTPS-secure cookies.

This is a single-operator application, not a multi-tenant SaaS. Team roles, invitations, account recovery and persistent distributed sessions are future work.

## Data persistence

Local mode uses file-backed H2 in PostgreSQL compatibility mode. PostgreSQL uses an explicit JDBC connection URL and the same migration. Compatibility mode is a development convenience, not a substitute for executing the tests against PostgreSQL. The CI workflow includes that additional run.

The frontend is packaged into the Spring Boot JAR for one-origin deployment. A React-only static deployment cannot provide the application's authentication, transactions or persistence.

## Reservation deadline transitions

Deadline comparison is `now >= reservation_expires_at_ms`, under the order row lock. Payment and cancellation take the same order-first / sorted-inventory lock order as the expiry worker. The persisted deadline, not an in-memory timer, identifies work after restart. The worker scans at most 100 IDs and calls a separately proxied transactional method for each order. A competing worker rechecks state after acquiring the lock and becomes a no-op. Exceptions roll back order, stock, movement and event changes. Late successful simulator events are retained for review without restoring inventory claims. V2 marks legacy unpaid reservations immediately due; historical paid states are preserved.
