# Hands-On Lab #3: Event Based Processing


Real-Time Processing with Apache Pulsar Functions
--
Apache Pulsar provides a serverless computing framework known as [Pulsar Functions](https://pulsar.apache.org/docs/next/functions-overview/)
that allows us to easily execute arbitrary code against messages that are ingested into a Pulsar topic. Functions can
filter, aggregate, and enrich the data, extracting relevant information such as price changes, trade volumes, and market
sentiment indicators, etc.


### 1️⃣ Start Event Processing

Based on the configuration of the Crypto Market Feed source connector, we are ingesting data from multiple channels.
The connector consumes these messages and publishes them as raw JSON String objects in the source topic. However, in order
to use this data for analysis, a schema must be assigned to each of these messages.

Therefore, our first Pulsar Function, [coinbase-websocket-feed-router](..%2F..%2Fcoinbase-functions%2Fcoinbase-websocket-feed-router), 
is responsible for transforming these raw JSON strings into the appropriate schema type based on the channel it came 
from, and routing them to different topics based on their contents.

![coinbase-router.png](..%2Fimages%2FLab3%2Fcoinbase-router.png)


```bash
./bin/pulsar/start-coinbase-feed-router.sh
```

This command starts the Crypto Market Data Feed routing function using the configuration details specified in the
[coinbase-router.yaml](..%2F..%2Finfrastructure%2Fpulsar%2Ffunctions%2Fconf%2Fcoinbase-router.yaml) configuration file.

You can verify that the Pulsar Function is working by running the following command to consume messages from one of the
Function's configured output topics. If it is working properly, you should see output similar to that shown here:

````bash
docker exec -it pulsar-broker sh -c   "./bin/pulsar-client consume -n 0 -p Earliest -s my-sub persistent://feeds/realtime/coinbase-ticker"

----- got message -----
key:[null], properties:[], content:{"sequence":58026240238,"product_id":"ETH-USD","price":3317.39,"open_24h":3278.83,"volume_24h":103947.516,"low_24h":3202.8,"high_24h":3369.43,"volume_30d":4280222.53668427,"best_bid":3317.39,"best_bid_size":0.03373329,"best_ask":3317.71,"best_ask_size":0.44106743,"side":"sell","time":"2024-04-03T19:41:53.315111Z","millis":1712173313315,"trade_id":510978124,"last_size":0.15075034}
----- got message -----
key:[null], properties:[], content:{"sequence":77242205012,"product_id":"BTC-USD","price":65791.66,"open_24h":65957.46,"volume_24h":15085.345,"low_24h":64500.0,"high_24h":66944.06,"volume_30d":650213.84849108,"best_bid":65788.94,"best_bid_size":0.00238747,"best_ask":65791.66,"best_ask_size":0.3191051,"side":"buy","time":"2024-04-03T19:41:53.324727Z","millis":1712173313324,"trade_id":626125270,"last_size":0.00741024}
----- got message -----
key:[null], properties:[], content:{"sequence":58026240510,"product_id":"ETH-USD","price":3318.11,"open_24h":3278.83,"volume_24h":103947.71,"low_24h":3202.8,"high_24h":3369.43,"volume_30d":4280222.73668427,"best_bid":3317.77,"best_bid_size":0.7567167,"best_ask":3318.12,"best_ask_size":0.00551561,"side":"sell","time":"2024-04-03T19:41:53.343614Z","millis":1712173313343,"trade_id":510978125,"last_size":0.2}

...
````
