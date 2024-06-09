# Hands-On Lab #7: Aggregations with Flink SQL

Aggregations in Flink
--------------------------------------
In Flink SQL, aggregations refer to the process of summarizing or combining multiple rows of data into a single value or
a set of values. This is typically done using aggregate functions, which perform calculations on a set of values to 
return a single scalar value. Aggregations are essential in SQL for producing summary reports, generating statistics, 
and making data more comprehensible.

Common aggregate functions include:

- `COUNT()`: Counts the number of rows in a set.
- `SUM()`: Calculates the total sum of a numeric column.
- `AVG()`: Computes the average value of a numeric column.
- `MIN()`: Finds the minimum value in a set.
- `MAX()`: Finds the maximum value in a set.

Windowing
--------------------------------------
"Windowing" is the technique of dividing a continuous stream of data into finite, manageable chunks, known as windows, 
to enable analysis and aggregation. Windowing is necessary for aggregations on streaming data because streaming data is 
continuous and unbounded, meaning it keeps flowing indefinitely. Without a mechanism to define finite portions of this 
endless stream, it would be impossible to perform meaningful aggregations. Windowing addresses this challenge by breaking
the continuous stream into manageable chunks, or windows, over which aggregations can be computed. Next, let's explore 
some of the more common windowing techniques using Flink SQL. 

### 1️⃣ Tumbling Windows
[Tumbling windows](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/dev/datastream/operators/windows/#tumbling-windows) are a type of windowing used in stream processing that divides a continuous data stream into fixed-size,
non-overlapping segments. Each event in the data stream belongs to exactly one window, making it easy to manage and 
understand. Tumbling windows are particularly useful for periodic reporting, batch-like processing, and scenarios where 
data needs to be aggregated over distinct, contiguous intervals.

For example, if you specify a tumbling window with a size of 5 minutes, the current window will be evaluated and a new
window will be started every five minutes.

![tumbling-windows.svg](..%2Fimages%2Ftumbling-windows.svg)

This query creates a view named RollingAveragePrice that calculates a rolling average price for each product within a 
5-minute window from the ticker table. Let's go ahead and run the following command inside the Flink SQL client session.

```sql
CREATE VIEW RollingAveragePrice AS
SELECT
    product_id,
    TUMBLE_START(`time`, INTERVAL '5' MINUTE) AS window_start,
    TUMBLE_END(`time`, INTERVAL '5' MINUTE) AS window_end,
    AVG(price) AS avg_price
FROM
    ticker
GROUP BY
    product_id, TUMBLE(`time`, INTERVAL '5' MINUTE);
```

Next, we can validate the query by selecting directly from the `RollingAveragePrice` view and watch the results change.

![RollingAvgPrice-Query.png](..%2Fimages%2FLab7%2FRollingAvgPrice-Query.png)

The following command creates a view that performs several aggregate calculations on the ticker table, grouping the data
into 1-minute tumbling windows.

```sql
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
```

Next, we can validate the query by selecting directly from the `ProductAggregates` view and watch the results change.

![ProductAggregates-Query.png](..%2Fimages%2FLab7%2FProductAggregates-Query.png)

Similarly, the following command creates a view that performs several aggregate calculations on the ticker_stats table, 
grouping the data into 1-minute tumbling windows.

```sql
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
```

Next, we can validate the query by selecting directly from the `ProductStats_Aggregates` view and watch the results change.

![ProductStatsAggregates-Query.png](..%2Fimages%2FLab7%2FProductStatsAggregates-Query.png)

#### Key Characteristics of Tumbling Windows
- Fixed Size: Tumbling windows have a fixed duration, specified by the user.
- Non-Overlapping: Each window is contiguous and does not overlap with the previous or next window.
- One-to-One Mapping: Each data point belongs to one and only one window.

#### Use Cases for Tumbling Windows
- Periodic Reporting: Aggregating data for regular intervals, such as hourly sales, daily temperatures, or monthly user sign-ups.
- Batch Processing: Processing data in fixed-size chunks, similar to traditional batch jobs but in a real-time streaming context.
- Event Counting: Counting events that occur within fixed periods, like the number of transactions per minute.

### 2️⃣ Sliding Windows
Sliding windows, also known as hopping windows, are a type of windowing mechanism used in stream processing that allows 
for overlapping windows to be defined over a data stream. Unlike tumbling windows, where each event belongs to exactly one
window, sliding windows can include the same event in multiple windows. This is useful for scenarios where you need a 
continuous, moving analysis of the data, such as calculating running averages, sums, or other metrics.

With Flink's [sliding windows](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/dev/datastream/operators/windows/#sliding-windows),
the size of the windows is configured by the window size parameter. An additional window slide parameter controls how
frequently a sliding window is started. Hence, sliding windows can be overlapping if the slide is smaller than the window
size. In this case elements are assigned to multiple windows.

![sliding-windows.svg](..%2Fimages%2Fsliding-windows.svg)

For example, you could have windows of size 10 minutes that slides by 5 minutes. With this you get every 5 minutes a
window that contains the events that arrived during the last 10 minutes.

This query creates a view named `SlidingWindowAvgPrice` that calculates the average price of each product within sliding 
windows over the ticker table. The sliding windows have a size of 10 minutes and slide (move forward) every 5 minutes.

```sql
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
```

We can validate the query by selecting directly from the `SlidingWindowAvgPrice` view and watch the results change.

![SlidingWindowAvgPrice-Query.png](..%2Fimages%2FLab7%2FSlidingWindowAvgPrice-Query.png)

Let's run a query that creates a view named `SlidingWindowEventCount` that calculates the count of events for each product within 
sliding windows over the ticker table. The sliding windows have a size of 15 minutes and slide (move forward) every 5 minutes.

```sql
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
```

We can validate the query by selecting directly from the `SlidingWindowEventCount` view and watch the results change.

![SlidingWindowEventCount-Query.png](..%2Fimages%2FLab7%2FSlidingWindowEventCount-Query.png)

#### Key Characteristics of Sliding Windows
- Fixed Size: The duration of each window is fixed.
- Overlap: Windows can overlap, meaning that a single data point can be part of multiple windows.
- Advance Interval: Windows slide (move forward) by a specified interval, which can be smaller than the window size, creating the overlap.

#### Use Cases for Sliding Windows
- Running Averages: Calculating moving averages over a set time period to smooth out short-term fluctuations.
- Trend Analysis: Detecting trends and changes in real-time data streams.
- Anomaly Detection: Monitoring metrics continuously to detect anomalies within overlapping intervals.
- Rolling Counts and Sums: Continuously counting or summing events over overlapping periods for real-time insights.

### 3️⃣ Lagging Windows
The LAG function in Apache Flink is a built-in window function that allows users to access data from a previous row in 
the same result set. It's useful for comparing values in the current row with values in a previous row, such as to 
analyze differences between consecutive rows or identify trends. For example, the `LAG` function can be used to calculate 
how much a stock price has increased or decreased over time.

To demonstrate this capability, let's run a query that creates a view named `PriceChanges` that calculates the percentage
change in price for each product over time in the `ticker` table. 

```sql
CREATE VIEW PriceChanges AS
SELECT
    product_id,
    price,
    `time`,
    LAG(price, 1) OVER (PARTITION BY product_id ORDER BY `time`) AS prev_price,
    ((price - LAG(price, 1) OVER (PARTITION BY product_id ORDER BY `time`)) / LAG(price, 1) OVER (PARTITION BY product_id ORDER BY `time`)) * 100 AS price_change_percentage
FROM
    ticker;
```

The query calculates the percentage change in price for each product between the current price and the previous price by
taking the difference between the current price and the previous price, dividing it by the previous price, and then 
multiplying by 100 to get the percentage change.

We can validate the query by selecting directly from the `PriceChanges` view and watch the results change.

![PriceChanges-Query.png](..%2Fimages%2FLab7%2FPriceChanges-Query.png)

Next, let's run a query that creates a view named `PriceSpikes` that filters out the rows from the `PriceChanges` view 
we created previously where the absolute value of the price change percentage is greater than 0.001. This allows us to 
quickly identify tickers that are experiencing rapid price changes.

```sql
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
```

We can validate the query by selecting directly from the `PriceSpikes` view and watch the results change.

![PriceSpikes-Query.png](..%2Fimages%2FLab7%2FPriceSpikes-Query.png)

Lastly, let's create creates a view named `VolumeSurges` that calculates the percentage change in volume for each product
over time in the ticker table.

```sql
CREATE VIEW VolumeSurges AS
SELECT
    product_id,
    volume_24h,
    `time`,
    LAG(volume_24h, 1) OVER (PARTITION BY product_id ORDER BY `time`) AS prev_volume,
    ((volume_24h - LAG(volume_24h, 1) OVER (PARTITION BY product_id ORDER BY `time`)) / LAG(volume_24h, 1) OVER (PARTITION BY product_id ORDER BY `time`)) * 100 AS volume_change_percentage
FROM
    ticker;
```

This query calculates the percentage change in volume for each product between the current volume and the previous volume,
by taking the difference between the current volume and the previous volume, dividing it by the previous volume, and then
multiplying by 100 to get the percentage change.

We can validate the query by selecting directly from the `VolumeSurges` view and watch the results change.

![VolumeSurges-Query.png](..%2Fimages%2FLab7%2FVolumeSurges-Query.png)

#### Key Characteristics of Lag Functions
- Access Previous Rows: The LAG function allows you to access data from previous rows within the same partition.
- Offset: You can specify how many rows back you want to look.
- Default Value: You can provide a default value if the specified row does not exist (e.g., when looking back more rows than are available).

#### Example Use Cases
- Calculating Differences: Finding the difference between current and previous values.
- Time Series Analysis: Comparing current values to past values to detect trends or anomalies.
- Sessionization: Identifying the start and end of sessions or detecting gaps in activity.


