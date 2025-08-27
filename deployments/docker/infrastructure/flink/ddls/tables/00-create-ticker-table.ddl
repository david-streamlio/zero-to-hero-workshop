CREATE TABLE ticker (
        price DOUBLE,
        last_size DOUBLE,
        sequence BIGINT,
        product_id STRING,
        open_24h DOUBLE,
        volume_24h DOUBLE,
        low_24h DOUBLE,
        high_24h DOUBLE,
        volume_30d DOUBLE,
        best_bid DOUBLE,
        best_bid_size DOUBLE,
        best_ask DOUBLE,
        best_ask_size DOUBLE,
        side STRING,
        `time` TIMESTAMP_LTZ(3),
        trade_id BIGINT,
        WATERMARK FOR `time` AS `time` - INTERVAL '5' SECOND
) WITH (
    'connector' = 'pulsar',
    'topics' = 'persistent://feeds/realtime/coinbase-ticker',
    'service-url' = 'pulsar://pulsar-broker:6650',
    'source.start.message-id' = 'earliest' ,
    'format' = 'json',
    'json.timestamp-format.standard' = 'ISO-8601'
);