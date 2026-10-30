CREATE TABLE products (
 id VARCHAR(36) PRIMARY KEY, sku VARCHAR(40) NOT NULL UNIQUE, name VARCHAR(140) NOT NULL,
 category VARCHAR(80) NOT NULL, price_cents BIGINT NOT NULL CHECK(price_cents>0), created_at VARCHAR(40) NOT NULL
);
CREATE TABLE warehouses(id VARCHAR(36) PRIMARY KEY,name VARCHAR(100) NOT NULL,location VARCHAR(100) NOT NULL);
CREATE TABLE inventory (
 warehouse_id VARCHAR(36) NOT NULL REFERENCES warehouses(id), product_id VARCHAR(36) NOT NULL REFERENCES products(id),
 on_hand INTEGER NOT NULL CHECK(on_hand>=0), reserved INTEGER NOT NULL DEFAULT 0 CHECK(reserved>=0),
 PRIMARY KEY(warehouse_id,product_id), CHECK(reserved<=on_hand)
);
CREATE TABLE orders (
 id VARCHAR(36) PRIMARY KEY, reference VARCHAR(30) NOT NULL UNIQUE, customer VARCHAR(140) NOT NULL,
 warehouse_id VARCHAR(36) NOT NULL REFERENCES warehouses(id), status VARCHAR(30) NOT NULL,
 total_cents BIGINT NOT NULL CHECK(total_cents>0), refunded_cents BIGINT NOT NULL DEFAULT 0,
 created_at VARCHAR(40) NOT NULL, updated_at VARCHAR(40) NOT NULL,
 CHECK(status IN ('RESERVED','PAID','FULFILLED','PARTIALLY_RETURNED','RETURNED','CANCELLED')),
 CHECK(refunded_cents>=0 AND refunded_cents<=total_cents)
);
CREATE TABLE order_lines (
 id VARCHAR(36) PRIMARY KEY, order_id VARCHAR(36) NOT NULL REFERENCES orders(id), product_id VARCHAR(36) NOT NULL REFERENCES products(id),
 sku VARCHAR(40) NOT NULL,name VARCHAR(140) NOT NULL,unit_price_cents BIGINT NOT NULL CHECK(unit_price_cents>0),
 quantity INTEGER NOT NULL CHECK(quantity>0),returned INTEGER NOT NULL DEFAULT 0 CHECK(returned>=0),
 UNIQUE(order_id,product_id), CHECK(returned<=quantity)
);
CREATE TABLE idempotency_keys (
 scope VARCHAR(100) NOT NULL, key_value VARCHAR(100) NOT NULL, fingerprint VARCHAR(64) NOT NULL,
 resource_id VARCHAR(36) NOT NULL, created_at VARCHAR(40) NOT NULL, PRIMARY KEY(scope,key_value)
);
CREATE TABLE payment_events (
 event_id VARCHAR(100) PRIMARY KEY, order_id VARCHAR(36) NOT NULL REFERENCES orders(id), amount_cents BIGINT NOT NULL,
 outcome VARCHAR(20) NOT NULL,created_at VARCHAR(40) NOT NULL
);
CREATE TABLE returns (
 id VARCHAR(36) PRIMARY KEY, order_id VARCHAR(36) NOT NULL REFERENCES orders(id), refund_cents BIGINT NOT NULL CHECK(refund_cents>0),
 reason VARCHAR(1000) NOT NULL,created_at VARCHAR(40) NOT NULL
);
CREATE TABLE return_lines (
 return_id VARCHAR(36) NOT NULL REFERENCES returns(id), order_line_id VARCHAR(36) NOT NULL REFERENCES order_lines(id),
 quantity INTEGER NOT NULL CHECK(quantity>0), PRIMARY KEY(return_id,order_line_id)
);
CREATE TABLE stock_movements (
 id VARCHAR(36) PRIMARY KEY, warehouse_id VARCHAR(36) NOT NULL REFERENCES warehouses(id), product_id VARCHAR(36) NOT NULL REFERENCES products(id),
 order_id VARCHAR(36) REFERENCES orders(id), kind VARCHAR(30) NOT NULL, on_hand_delta INTEGER NOT NULL,
 reserved_delta INTEGER NOT NULL, reason VARCHAR(1000) NOT NULL,created_at VARCHAR(40) NOT NULL
);
CREATE TABLE activity(id VARCHAR(36) PRIMARY KEY,order_id VARCHAR(36) REFERENCES orders(id),action VARCHAR(50) NOT NULL,detail VARCHAR(1000) NOT NULL,created_at VARCHAR(40) NOT NULL);
CREATE INDEX orders_created ON orders(created_at);
CREATE INDEX movements_created ON stock_movements(created_at);
CREATE INDEX lines_order ON order_lines(order_id);
CREATE INDEX activity_created ON activity(created_at);

ALTER TABLE payment_events ADD COLUMN effect VARCHAR(30) NOT NULL;
CREATE TABLE operation_locks(bucket INTEGER PRIMARY KEY);
INSERT INTO operation_locks VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9),(10),(11),(12),(13),(14),(15),(16),(17),(18),(19),(20),(21),(22),(23),(24),(25),(26),(27),(28),(29),(30),(31),(32),(33),(34),(35),(36),(37),(38),(39),(40),(41),(42),(43),(44),(45),(46),(47),(48),(49),(50),(51),(52),(53),(54),(55),(56),(57),(58),(59),(60),(61),(62),(63);
