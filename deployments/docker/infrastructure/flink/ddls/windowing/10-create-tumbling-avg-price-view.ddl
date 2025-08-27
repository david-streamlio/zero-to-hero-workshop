CREATE VIEW RollingAveragePrice AS
SELECT
    product_id,
    TUMBLE_START(`time`, INTERVAL '5' MINUTE) AS window_start,
    TUMBLE_END(`time`, INTERVAL '5' MINUTE) AS window_end,
    AVG(price) OVER (PARTITION BY product_id ORDER BY `time` RANGE BETWEEN INTERVAL '5' MINUTE PRECEDING AND CURRENT ROW)
        AS rolling_avg_price
FROM
    ticker;