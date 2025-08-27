CREATE VIEW SlidingWindowAvgPrice AS
SELECT
    product_id,
    HOP_START(`time`, INTERVAL '5' MINUTE, INTERVAL '10' MINUTE) AS window_start,
    HOP_END(`time`, INTERVAL '5' MINUTE, INTERVAL '10' MINUTE) AS window_end,
    AVG(price) AS avg_price
FROM
    ticker
GROUP BY
    product_id,
    HOP(`time`, INTERVAL '5' MINUTE, INTERVAL '10' MINUTE);