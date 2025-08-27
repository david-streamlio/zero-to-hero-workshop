CREATE TABLE ticker_stats (
    price DOUBLE,
    latest_emwa DOUBLE,
    latest_std DOUBLE,
    latest_variance DOUBLE,
    rolling_mean DOUBLE,
    rolling_std DOUBLE,
    rolling_variance DOUBLE,
    sequence BIGINT,
    product_id STRING,
    `time` TIMESTAMP_LTZ(3),
    WATERMARK FOR `time` AS `time` - INTERVAL '5' SECOND
) WITH (
    'connector' = 'pulsar',
    'topics' = 'persistent://feeds/realtime/coinbase-ticker-stats',
    'service-url' = 'pulsar://pulsar-broker:6650',
    'source.start.message-id' = 'earliest' ,
    'format' = 'json',
    'json.timestamp-format.standard' = 'ISO-8601'
);