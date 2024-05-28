CREATE TABLE ticker_stats (
    price DOUBLE,
    latest_ewma DOUBLE,
    latest_std DOUBLE,
    latest_variance DOUBLE,
    rolling_mean DOUBLE,
    rolling_std DOUBLE,
    rolling_variance DOUBLE,
    sequence BIGINT,
    product_id STRING,
    `time` TIMESTAMP_LTZ(3)
) WITH (
    'connector' = 'pulsar',
    'topics' = 'persistent://feeds/realtime/coinbase-ticker-stats',
    'service-url' = 'pulsar://pulsar:6650',
    'source.start.message-id' = 'earliest' ,
    'format' = 'json',
    'json.timestamp-format.standard' = 'ISO-8601'
);