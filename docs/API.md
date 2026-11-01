# HTTP API

All business endpoints require an authenticated operator session. Responses use JSON. Database-field responses use snake_case; request contracts use camelCase.

## Session

`GET /api/session` is public and returns `authenticated`, `username` and `csrfToken`. Send the token in `X-CSRF-TOKEN` for all mutations, including login. `POST /api/login` accepts URL-encoded `username` and `password`. Fetch `/api/session` again after login to obtain the rotated token. `POST /api/logout` invalidates the session.

`GET /api/health` is public and identifies the app and simulated payment mode.

## Read endpoints

- `GET /api/overview`: inventory totals and simulated paid order value less recorded refunds.
- `GET /api/products`: catalog.
- `GET /api/warehouses`: fulfillment locations.
- `GET /api/inventory`: on-hand, reserved and available units per position.
- `GET /api/orders`: order list.
- `GET /api/orders/{id}`: snapshots, lines, payments, returns and timeline.
- `GET /api/movements`: latest 200 stock ledger entries.
- `GET /api/activity`: latest 100 activity entries.

## Product and warehouse setup

`POST /api/products`:

```json
{"sku":"AUDIO-001","name":"Studio headphones","category":"Audio","priceCents":12900}
```

`POST /api/warehouses`:

```json
{"name":"Hong Kong studio","location":"Hong Kong SAR"}
```

Each new product/warehouse initializes corresponding zero-stock positions.

## Retry-safe mutations

Stock receipts, order creation and returns require `Idempotency-Key`, containing 8–100 letters, numbers, dots, colons or hyphens. Retain the same key for a retry of identical input. Use a new key for a new intended operation. Changed payload under a used key returns 409.

`POST /api/inventory/receive`:

```json
{"warehouseId":"WAREHOUSE_ID","productId":"PRODUCT_ID","quantity":10,"reason":"Supplier delivery PO-024"}
```

`POST /api/orders`:

```json
{"customer":"Example customer","warehouseId":"WAREHOUSE_ID","lines":[{"productId":"PRODUCT_ID","quantity":2}]}
```

Stock conflicts return 409 and leave no partial reservation. Duplicate product lines are rejected.

`POST /api/orders/{id}/returns`:

```json
{"reason":"Customer returned one item","lines":[{"lineId":"ORDER_LINE_ID","quantity":1}]}
```

## Order transitions

`POST /api/orders/{id}/cancel` cancels only unpaid reservations. Repeating cancellation does not release stock twice.

`POST /api/orders/{id}/fulfill` ships a paid order. Repeating an already completed fulfillment does not deduct stock twice.

## Simulated payment events

`POST /api/payments/simulate`:

```json
{"eventId":"provider-simulation-0001","orderId":"ORDER_ID","amountCents":25800,"outcome":"SUCCEEDED"}
```

Outcome is `SUCCEEDED` or `FAILED`. Event IDs follow the same 8–100 character key format. The amount must match the order total. The response contains the recorded event, current order and a replay indicator. Conflicting reuse returns 409. This endpoint is an operator-controlled simulator, not an externally authenticated payment-provider webhook.

## Error semantics

400: invalid input or missing/invalid operation key. 401: unauthenticated. 403: missing or invalid CSRF verification. 404: missing record. 409: stock conflict, prohibited state transition, invalid return quantity, key conflict, uniqueness violation or lock contention.

After lock contention or an uncertain network response, retry the exact input with the same idempotency key. Do not automatically retry a changed business action under the old key.

## Reservation evidence

Order responses now include `reservation_expires_at_ms` (epoch milliseconds, nullable for historical states), `cancellation_reason`, `cancelled_at` and `payment_review_events`. Expiry preserves status `CANCELLED` and uses reason `RESERVATION_EXPIRED`; manual cancellation uses `OPERATOR_CANCELLED`. Late simulated successes return effect `LATE_SUCCESS_REVIEW`; late failures return `IGNORED_EXPIRED`. These are recorded simulator decisions, not confirmation of external settlement or refund. There is no public endpoint to alter an existing deadline or advance the server clock.
