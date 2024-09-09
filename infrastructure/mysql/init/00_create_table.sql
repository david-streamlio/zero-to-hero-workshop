
CREATE TABLE ticker (
    product_id VARCHAR(255) PRIMARY KEY,
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