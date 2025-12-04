CREATE TABLE IF NOT EXISTS stock (
    product_id VARCHAR(100) PRIMARY KEY,
    available_quantity INTEGER NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS reservations (
    reservation_id UUID PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL UNIQUE,
    product_id VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_reservation_stock FOREIGN KEY (product_id) REFERENCES stock(product_id)
);

CREATE INDEX IF NOT EXISTS idx_reservations_product ON reservations(product_id);
