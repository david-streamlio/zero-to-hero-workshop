CREATE VIEW PriceChanges AS
SELECT
    product_id,
    price,
    `time`,
    LAG(price, 1) OVER (PARTITION BY product_id ORDER BY `time`) AS prev_price,
    ((price - LAG(price, 1) OVER (PARTITION BY product_id ORDER BY `time`)) / LAG(price, 1) OVER (PARTITION BY product_id ORDER BY `time`)) * 100 AS price_change_percentage
FROM
    ticker;