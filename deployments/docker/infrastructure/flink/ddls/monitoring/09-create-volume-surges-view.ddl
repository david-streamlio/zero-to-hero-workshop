CREATE VIEW VolumeSurges AS
SELECT
    product_id,
    volume_24h,
    `time`,
    LAG(volume_24h, 1) OVER (PARTITION BY product_id ORDER BY `time`) AS prev_volume,
    ((volume_24h - LAG(volume_24h, 1) OVER (PARTITION BY product_id ORDER BY `time`)) / LAG(volume_24h, 1) OVER (PARTITION BY product_id ORDER BY `time`)) * 100 AS volume_change_percentage
FROM
    ticker;