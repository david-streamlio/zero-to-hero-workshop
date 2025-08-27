CREATE VIEW PriceSpikes AS
SELECT
    product_id,
    price,
    `time`,
    price_change_percentage
FROM
    PriceChanges
WHERE
    ABS(price_change_percentage)  > 0.001;
