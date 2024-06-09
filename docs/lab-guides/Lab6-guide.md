# Hands-On Lab #6: Ad-hoc Queries with Flink SQL

Flink SQL
--------------------------------------

[Flink SQL](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/table/sql/overview/) is an ANSI standard compliant SQL engine 
that can process both real-time and historical data. It allows users to express data transformations and analytics on streams of data 
using the familiar SQL syntax.


### 1️⃣ Start Flink SQL

The Flink SQL Client aims to provide an easy way of writing, debugging, and submitting table programs to a Flink cluster
without a single line of Java or Scala code. The SQL client makes it possible to work with queries written in the SQL language
to manipulate streaming data.

Users have two options for starting the SQL Client CLI, either by starting an embedded standalone process inside a shell
using the following command:

```bash
sh ./bin/flink/start-sql-client.sh
```

Alternatively, you can exec directly into the sql-client-1 container using Docker Desktop, and executing the following
command to start the SQL client:

```bash
docker exec -it flink-sql-client-1 /bin/bash
```

Then inside that shell run:

```
./bin/sql-client.sh embedded -l /opt/sql-client/lib
```

![Flink-SQL-UI.png](..%2Fimages%2FLab6%2FFlink-SQL-UI.png)


### 2️⃣ Create tables for the Pulsar Topics

In order to access the data inside Apache Pulsar, you must first create table definitions inside the Flink SQL session 
that are configured to point to the correct topic. Let's run the following commands inside the Flink SQL shell:

The following command exposes the data inside the `persistent://feeds/realtime/coinbase-ticker` topic as a table in Flink.
```
CREATE TABLE ticker (
        price DOUBLE,
        last_size DOUBLE,
        sequence BIGINT,
        product_id STRING,
        open_24h DOUBLE,
        volume_24h DOUBLE,
        low_24h DOUBLE,
        high_24h DOUBLE,
        volume_30d DOUBLE,
        best_bid DOUBLE,
        best_bid_size DOUBLE,
        best_ask DOUBLE,
        best_ask_size DOUBLE,
        side STRING,
        `time` TIMESTAMP_LTZ(3),
        trade_id BIGINT,
        WATERMARK FOR `time` AS `time` - INTERVAL '5' SECOND
) WITH (
    'connector' = 'pulsar',
    'topics' = 'persistent://feeds/realtime/coinbase-ticker',
    'service-url' = 'pulsar://pulsar-broker:6650',
    'source.start.message-id' = 'earliest' ,
    'format' = 'json',
    'json.timestamp-format.standard' = 'ISO-8601'
);
```

and this command exposes the data inside the `persistent://feeds/realtime/coinbase-ticker-stats` as a table inside Flink

```
CREATE TABLE ticker_stats (
    price DOUBLE,
    latest_emwa DOUBLE,
    latest_std DOUBLE,
    latest_variance DOUBLE,
    rolling_mean DOUBLE,
    rolling_std DOUBLE,
    rolling_variance DOUBLE,
    sequence BIGINT,
    product_id STRING,
    `time` TIMESTAMP_LTZ(3),
    WATERMARK FOR `time` AS `time` - INTERVAL '5' SECOND
) WITH (
    'connector' = 'pulsar',
    'topics' = 'persistent://feeds/realtime/coinbase-ticker-stats',
    'service-url' = 'pulsar://pulsar-broker:6650',
    'source.start.message-id' = 'earliest' ,
    'format' = 'json',
    'json.timestamp-format.standard' = 'ISO-8601'
);
```

### 3️⃣ Validate
The next step is to confirm that the table definitions exist in the Flink SQl catalog. This allows us to run queries
against the tables. Running the following command should yield the results you see here.

```
Flink SQL> show tables;
+--------------+
|   table name |
+--------------+
|       ticker |
| ticker_stats |
+--------------+
2 rows in set
```

Now we know that the tables exist in Flink, and we can query them using Flink SQL

Next, we will want to confirm that we are able to read the data from Apahce Pulsar using the Flink SQL client. This
validates the table definitions are correct, e.g., pointing to the correct Pulsar broker URL and topic, etc. 

Let's start with a simple query to confirm that we can access the data in ticker table:

`select * from ticker;`

This starts a continuous query on the ticker table that automatically refreshes every second. Thus, you should see results similar to the one
show below, where there is a list of constantly changing values. When you are done watching the values change, you can hit `Q` to exit

![Flink-SQL-ticker-table-sql.png](..%2Fimages%2FLab6%2FFlink-SQL-ticker-table-sql.png)

We can do the same for the `ticker_stats` table as well to confirm that the table definition is correct, and data is available in Pulsar.
Running `select * from ticker_stats;` should yield results similar to that shown here.

![Flink-SQL-ticker-stats-table-query.png](..%2Fimages%2FLab6%2FFlink-SQL-ticker-stats-table-query.png)
Before hitting `Q` to terminate the query, open up the [Flink UI](http://localhost:8081/#/job/running) and confirm that there is a corresponding
Flink job running for the query.

![Flink-SQL-Job-in-UI.png](..%2Fimages%2FLab6%2FFlink-SQL-Job-in-UI.png)
If you double-clink on the Job link, you can also see some of the details of the associated Flink Job

![Flink-SQL-Job-Details-UI.png](..%2Fimages%2FLab6%2FFlink-SQL-Job-Details-UI.png)

### 4️⃣ Ad-Hoc queries

Now that you have confirmed that the Flink tables are properly defined, you are free to run some exploratory
SQL queries on the data, such as:

`select * from ticker T LEFT JOIN ticker_stats S on T.sequence = S.sequence;`

