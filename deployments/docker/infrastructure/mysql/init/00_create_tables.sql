
CREATE TABLE current_price (
    product_id VARCHAR(30) PRIMARY KEY,
    ts TIMESTAMP,
    price FLOAT,
    open_24h FLOAT,
    volume_24h FLOAT,
    low_24h FLOAT,
    high_24h FLOAT,
    volume_30d DOUBLE,
    best_bid FLOAT,
    best_bid_size FLOAT,
    best_ask FLOAT,
    best_ask_size FLOAT
);

CREATE TABLE price_history (
    product_id VARCHAR(30),
    ts TIMESTAMP,
    price FLOAT
)