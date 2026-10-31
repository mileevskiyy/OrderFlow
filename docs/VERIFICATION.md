# Verification record

## Connected QA upgrade — 31 October 2026

- `npm run build --prefix frontend`: passed.
- `node scripts/qualityhub-ci.mjs`: passed against the packaged Java service and sibling QualityHub checkout.
- A controlled HTTP 503 was actually injected at order creation; its browser invocation failed as required. The same workflow without the injection passed.
- Both completed runs arrived through the reporter with seven actual screenshot/log/error-context/trace files. Each download matched its stored SHA-256; repeated report/file delivery returned replay responses.
- A linked defect was created and the completed-run comparison showed one fixed test. Reports and exports are written under ignored `.qualityhub-evidence/`.
- The local run records uncommitted tracked changes as dirty. This experiment does not represent a production outage or real deployed fix.
- The Java business logic and payment-provider scope are unchanged. Remote CI results are recorded separately after execution.

The initial-release record below is historical.


Initial release checked on 30 October 2026 using Java 21.0.4 and Node.js 24.17.0.

## Executed checks

- Production frontend build: TypeScript checks and Vite asset compilation passed.
- Maven package: executable Spring Boot JAR built successfully with frontend assets.
- JUnit/Spring suite: 11 tests passed, 0 failures and 0 errors.
- Playwright Chromium workflow: 1 test passed, exercising the actual Java service and packaged interface.
- A separate seeded-browser check passed and captured the desktop preview; the local interface was also inspected at a narrow viewport.
- Frontend dependency installation audit reported no known vulnerabilities at installation time.

## Domain evidence

The tests verify separation of physical and reserved quantities; identical-order replay; conflicting-key rejection; two requests contending for one remaining unit; simultaneous duplicate delivery; rollback of multi-line shortages; repeated/out-of-order simulated payments; cancellation; paid-only fulfillment; duplicate-shipment protection; partial returns; refund calculations from historical prices; return limits; receipt replay; and reconciliation of ledger deltas with inventory balances.

The HTTP security test starts an actual embedded server. It verifies private-endpoint authentication, login with a session CSRF token, access after login, rejection of fractional cent values and rejection of a mutation without CSRF verification.

## Browser evidence

Starting with an empty isolated H2 database, the automated browser workflow signs in, creates a product, creates a warehouse, receives five units, reserves two units, confirms simulated payment, fulfills the order, returns one unit and reloads the workspace. It then verifies physical stock of four, reserved stock of zero and available stock of four.

The browser test uses synthetic credentials and records. The normal preview contains labelled fictional demo data. Neither set represents real customers, fulfilled shipments or financial transactions.

## Verification limits

Local database tests used H2 in PostgreSQL compatibility mode. The PostgreSQL driver and Flyway migration are included and the GitHub Actions workflow provisions PostgreSQL to run the same domain suite, but a live PostgreSQL server and remote CI have not yet been exercised for this repository in this environment.

The Docker Compose stack, production HTTPS configuration and public deployment have not been run here. No performance benchmark, real payment-provider integration, multi-user security assessment or production adoption is claimed. Generated/static assets, dependency caches and local databases are excluded from the source archive.
