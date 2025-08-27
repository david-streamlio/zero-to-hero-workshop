CREATE VIEW ProductStats_Aggregates AS
SELECT
    product_id,
    TUMBLE_START(`time`, INTERVAL '1' MINUTE) AS window_start,
    TUMBLE_END(`time`, INTERVAL '1' MINUTE) AS window_end,
    AVG(latest_emwa) AS avg_emwa,
    MIN(latest_emwa) AS min_emwa,
    MAX(latest_emwa) AS max_emwa,
    AVG(latest_variance) AS avg_variance,
    MIN(latest_variance) AS min_variance,
    MAX(latest_variance) AS max_variance
FROM
    ticker_stats
GROUP BY
    product_id,
    TUMBLE(`time`, INTERVAL '1' MINUTE);