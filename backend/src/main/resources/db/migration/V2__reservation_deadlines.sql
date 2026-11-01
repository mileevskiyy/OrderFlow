ALTER TABLE orders ADD COLUMN reservation_expires_at_ms BIGINT;
ALTER TABLE orders ADD COLUMN cancellation_reason VARCHAR(40);
ALTER TABLE orders ADD COLUMN cancelled_at VARCHAR(40);
-- Legacy unpaid reservations have no trustworthy deadline. Release them on the first sweep.
UPDATE orders SET reservation_expires_at_ms=0 WHERE status='RESERVED';
CREATE INDEX orders_due_reservations ON orders(status,reservation_expires_at_ms,id);
