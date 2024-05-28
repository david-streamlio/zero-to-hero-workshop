# Hands-On Lab #8: Materialized Streams

Prerequisites
------------

- [Lab1](Lab1-guide.md)
- [Lab2](Lab2-guide.md)
- [Lab3](Lab3-guide.md)
- [Lab4](Lab4-guide.md)
- [Lab5](Lab5-guide.md)
- [Lab6](Lab6-guide.md)
- [Lab7](Lab7-guide.md)


### Source code

[TickerFeatureExtractionJob.java](..%2F..%2Fcoinbase-flink%2Fcoinbase-joins%2Fsrc%2Fmain%2Fjava%2Fio%2Fstreamnative%2Fcoinbase%2Fflink%2Fjoins%2Fstream%2FTickerFeatureExtractionJob.java)

### Build it

### Run it


```
docker exec -ti jobmanager /bin/bash

./bin/flink run /jobs/coinbase-joins-TickerFeatureExtractionJob.jar \
   --broker-service-url pulsar://pulsar:6650 \
   --admin-service-url http://pulsar:8080
```

### Verify

View the pulsar topic with the new data


