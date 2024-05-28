CREATE TABLE ticker_features
WITH (
    'connector' = 'pulsar',
    'topics' = 'persistent://feeds/realtime/coinbase-ticker-features',
    'service-url' = 'pulsar://pulsar-broker:6650',
    'format' = 'json',
    'json.timestamp-format.standard' = 'ISO-8601'
)
AS
SELECT T.price,
       T.`time`,
       T.last_size,
       T.best_bid,
       T.best_ask,
       T.best_bid_size - T.best_ask_size AS spread,
       S.latest_emwa,
       S.latest_std,
       S.latest_variance,
       S.rolling_mean,
       S.rolling_std,
       S.rolling_variance
    FROM ticker T
    LEFT JOIN ticker_stats S
        ON T.sequence = S.sequence;