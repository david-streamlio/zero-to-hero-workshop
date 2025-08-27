CREATE VIEW SlidingWindowEventCount AS
SELECT
    product_id,
    HOP_START(`time`, INTERVAL '5' MINUTE, INTERVAL '15' MINUTE) AS window_start,
    HOP_END(`time`, INTERVAL '5' MINUTE, INTERVAL '15' MINUTE) AS window_end,
    COUNT(*) AS event_count
FROM
    ticker
GROUP BY
    product_id,
    HOP(`time`, INTERVAL '5' MINUTE, INTERVAL '15' MINUTE);