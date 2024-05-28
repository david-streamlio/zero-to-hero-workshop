# Hands-On Lab #6: Ad-hoc Queries with Flink SQL

Prerequisites
------------

- [Lab1](Lab1-guide.md)
- [Lab2](Lab2-guide.md)
- [Lab3](Lab3-guide.md)
- [Lab4](Lab4-guide.md)
- [Lab5](Lab5-guide.md)

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
./bin/sql-client.sh embedded -l /opt/sql-client/lib
```

![Flink-SQL-UI.png](..%2Fimages%2FFlink-SQL-UI.png)


### 2️⃣ Create tables for the Pulsar Topics

In order to access the data inside Apache Pulsar, you must first 

[00-create-ticker-table.ddl](..%2F..%2Finfrastructure%2Fflink%2Fddls%2Ftables%2F00-create-ticker-table.ddl)
[01-create-ticker-stats-table.ddl](..%2F..%2Finfrastructure%2Fflink%2Fddls%2Ftables%2F01-create-ticker-stats-table.ddl)

### Verification

`show tables;`

### Ad-Hoc queries

Then you can perform some ad-hoc queries on these tables.

`select * from ticker;`

You should see a corresponding Flink job in the Flink UI

`select * from ticker_stats;`

`select * from ticker T LEFT JOIN ticker_stats S on T.sequence = S.sequence;`

