CREATE TABLE rfq_match (
    price DOUBLE,
    size DOUBLE,
    product_id STRING,
    side STRING,
    `time` TIMESTAMP_LTZ(6),
    maker_order_id STRING,
    taker_order_id STRING,
    proctime AS PROCTIME()
) WITH (
    'connector' = 'pulsar',
    'topics' = 'persistent://feeds/realtime/coinbase-rfq',
    'service-url' = 'pulsar://pulsar-broker:6650',
    'source.start.message-id' = 'earliest' ,
    'format' = 'json',
    'json.timestamp-format.standard' = 'ISO-8601'
);