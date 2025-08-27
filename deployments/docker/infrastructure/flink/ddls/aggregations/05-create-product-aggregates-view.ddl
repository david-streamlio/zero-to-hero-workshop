CREATE VIEW ProductAggregates AS
SELECT
    product_id,
    TUMBLE_START(`time`, INTERVAL '1' MINUTE) AS window_start,
    TUMBLE_END(`time`, INTERVAL '1' MINUTE) AS window_end,
    AVG(price) AS avg_price,
    MIN(price) AS min_price,
    MAX(price) AS max_price,
    COUNT(price) AS trades
FROM
    ticker
GROUP BY
    product_id,
    TUMBLE(`time`, INTERVAL '1' MINUTE);