Workshop Outline

Prerequisities
    Docker with 16 GB RAM / 10 cores
    IDE of some sort
    Browser

Lecture - Origin of Streaming
Types of problems streaming solves
    Analytics (Look at same data from different angles)
    Anomaly detection (Look for needles in a haystack)
Stream Storage
    - Partitions vs. Segments
    - Pulsar vs. Kafka

Use Case introduction (Coinbase planning slide with animations)
Hands-on
1. Start Pulsar
2. Start the Coinbase Live feed connector
3. Examine the topic data
4. Start the Coinbase Router function
5. List the topics
6. Examine the topic data

7. Optional (change the feed config)
8. Start the ticker stats function

Break

Lecture - Stream Processing
Stream processing architecture (RTA graphic of components)
Intro to Flink
    - Architecture Guide

9. Start Flink
   10. Flink Web UI
10. Flink SQL
    11. Create tables (DDLs 00 & 01) 
    12. Ad-hoc queries (show corresponding Flink Jobs in UI)
    13. Create Views  (05, 06, 07, 08)
    14. Create Monitoring on top of Views (TODO)

Break

Lecture
Categories of Stream Processing (RTA graphic)
Four different APIs

Table API / DataStream API - Hands on
15. Flink DataStream API
    16. Filters
    17. Aggregates
    18. Windowing
    19. Joins

    

```
docker exec -ti jobmanager /bin/bash

./bin/flink run /jobs/coinbase-filters-FilterTickerByProductId.jar \
   --broker-service-url pulsar://pulsar:6650 \
   --admin-service-url http://pulsar:8080 \
   --productId BTC-USD \
   --output-path /tmp/filterBTC2
   
./bin/flink run /jobs/coinbase-filters-MultiFilterTickerStats.jar \
   --broker-service-url pulsar://pulsar:6650 \
   --admin-service-url http://pulsar:8080 \
   --price 200.00 \
   --variance 10.0 \
   --output-path /tmp/filterPriceAndVariance
```

Joins

```
docker exec -ti jobmanager /bin/bash

./bin/flink run /jobs/coinbase-joins-TickerFeatureExtractionJob.jar \
   --broker-service-url pulsar://pulsar:6650 \
   --admin-service-url http://pulsar:8080
```

Flink SQL

```
docker exec -ti flink-sql-client-1 /bin/bash

./bin/sql-client.sh embedded -l /opt/sql-client/lib

SQL>
```