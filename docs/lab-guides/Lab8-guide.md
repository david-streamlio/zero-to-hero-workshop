# Hands-On Lab #8: Materialized Streams

Materialized Streams
------------------------------
Materialized streams, also known as materialized views or materialized query tables (MQTs), are a concept in data 
processing and streaming systems. They are a way of persisting the results of stream processing operations so that they 
can be queried repeatedly without recalculating the results every time.

In the context of streaming data, materialized streams are essentially stored snapshots of the stream data at specific 
points in time or based on specific conditions. These snapshots are materialized in a persistent storage layer, such as 

### Materialized streams offer several benefits:

- **Query Performance**: By precomputing and storing results, materialized streams can significantly improve query performance, especially for complex and computationally expensive operations.
- **Offline Analysis**: Once materialized, the stream data can be analyzed offline without the need for the original stream source, enabling historical analysis and reporting.
- **Real-time Aggregations**: Materialized streams can be used to maintain real-time aggregations or summaries of streaming data, providing insights into trends and patterns as they emerge.
- **Fault Tolerance**: Storing intermediate results provides fault tolerance in case of system failures or restarts. Data can be easily recovered from the materialized view without needing to reprocess the entire stream.
- **Reduced Resource Consumption**: By storing intermediate results, materialized streams reduce the computational resources required for repeated queries, as they avoid recomputation of the same results.


Joining the ticker and ticker_stats tables
--------------------------------------
We have two streaming data sources that share a natural key with one another, e.g. `sequence_id`. We want to create a 
JOIN these two datasets and store the results in a materialized view.  

![Materialize.png](..%2Fimages%2FLab8%2FMaterialize.png)


### 1️⃣  Source code

We will leverage Flinks DataStream API for this purpose. The Java source code is available here [TickerFeatureExtractionJob.java](..%2F..%2Fcoinbase-flink%2Fcoinbase-joins%2Fsrc%2Fmain%2Fjava%2Fio%2Fstreamnative%2Fcoinbase%2Fflink%2Fjoins%2Fstream%2FTickerFeatureExtractionJob.java)
As you can see, we are using a Broadcast Join to join both of these datasets together on the `sequence_id` field.

In a broadcast join, one of the input streams is broadcasted to all parallel instances of the processing task, while the
other input stream remains partitioned across the parallel instances. Each parallel instance then performs a join operation 
between the broadcasted stream and its local partition of the other stream.

By broadcasting one of the input streams, a broadcast join minimizes the amount of data movement and network communication
required during the join operation, leading to improved performance and reduced resource consumption.

In Apache Flink, broadcast joins can be implemented using the `broadcast()` method on the smaller stream before performing
the join operation. This instructs Flink to broadcast the specified stream to all processing instances for efficient join processing.

### 2️⃣ Build it

To build the jar file that contains the Flink Job definition, you need to run the following command:

```
cd coinbase-flink
mvn clean install
```

If everything goes as expected, and no errors are found you should output similar to that shown below.

```
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for coinbase-flink 0.0.1:
[INFO] 
[INFO] coinbase-flink ..................................... SUCCESS [  0.634 s]
[INFO] coinbase-schemas ................................... SUCCESS [  7.321 s]
[INFO] coinbase-flink-utils ............................... SUCCESS [  2.060 s]
[INFO] coinbase-joins ..................................... SUCCESS [ 10.362 s]
[INFO] coinbase-filters ................................... SUCCESS [ 11.346 s]
[INFO] coinbase-aggregations .............................. SUCCESS [  7.603 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  39.538 s
[INFO] Finished at: 2024-06-08T10:46:25+02:00
[INFO] ------------------------------------------------------------------------
```

Also, a jar file named `coinbase-joins-TickerFeatureExtractionJob.jar` will be generated and places in a directory that 
is already mounted to the jobManager pod inside your Flink cluster. This allows us to easily run the Flink Job from the 
command line.

### 3️⃣ Run it

To run the Flink Job, you must submit it to the JobManager. To do this, you must first exec into the `jobmanager` container
using the command below

```
docker exec -ti jobmanager /bin/bash
```

Once you are inside the jobmanager, you can execute the following command to launch the Flink Job:

```
./bin/flink run /jobs/coinbase-joins-TickerFeatureExtractionJob.jar \
   --broker-service-url pulsar://pulsar:6650 \
   --admin-service-url http://pulsar:8080
```

You can then switch to the Flink UI to check on the status of the Flink Job, look for errors, etc.

![FlinkJobInIUI.png](..%2Fimages%2FLab8%2FFlinkJobInIUI.png)

You can also drill down into the Flink Job details in the UI as well.

![FlinkJob-Details-View.png](..%2Fimages%2FLab8%2FFlinkJob-Details-View.png)

### 4️⃣  Verify

You can confirm that the Flink Job is working by checking the target Pulsar topic to see if any data is getting published.

First exec into the Pulsar Broker container using the following command.
```
docker exec -it pulsar-broker /bin/bash
```

Then execute the following command to get the statistics for the target Pulsar topic. You should see output similar to 
that shown below, which indicates that messages are being published to the topic at a rate of ~3.5 per second.

It also indicates that there is a single active message producer on the topic with a `producerName` that starts with "flink"

```
./bin/pulsar-admin topics stats persistent://feeds/realtime/ticker-features-partition-0
{
  "msgRateIn" : 3.550134297083622,
  "msgThroughputIn" : 1632.0284042339629,
  "msgRateOut" : 0.0,
  "msgThroughputOut" : 0.0,
  "bytesInCounter" : 764703,
  "msgInCounter" : 1637,
  "bytesOutCounter" : 0,
  "msgOutCounter" : 0,
  "averageMsgSize" : 459.7089201877934,
  "msgChunkPublished" : false,
  "storageSize" : 768587,
  "backlogSize" : 0,
  "publishRateLimitedTimes" : 0,
  "earliestMsgPublishTimeInBacklogs" : 0,
  "offloadedStorageSize" : 0,
  "lastOffloadLedgerId" : 0,
  "lastOffloadSuccessTimeStamp" : 0,
  "lastOffloadFailureTimeStamp" : 0,
  "ongoingTxnCount" : 0,
  "abortedTxnCount" : 0,
  "committedTxnCount" : 0,
  "publishers" : [ {
    "accessMode" : "Shared",
    "msgRateIn" : 3.550134297083622,
    "msgThroughputIn" : 1632.0284042339629,
    "averageMsgSize" : 459.7089201877934,
    "chunkedMessageRate" : 0.0,
    "producerId" : 0,
    "supportsPartialProducer" : false,
    "producerName" : "flink-ticker-features-sink-bbf1e34c-ca4f-4d90-9da1-52c0ced49c5c",
    "address" : "/172.18.0.6:42140",
    "connectedSince" : "2024-06-09T05:38:26.542406585Z",
    "clientVersion" : "Pulsar-Java-v3.0.0",
    "metadata" : { }
  } ],
  "waitingPublishers" : 0,
  "subscriptions" : { },
  "replication" : { },
  "deduplicationStatus" : "Disabled",
  "nonContiguousDeletedMessagesRanges" : 0,
  "nonContiguousDeletedMessagesRangesSerializedSize" : 0,
  "delayedMessageIndexSizeInBytes" : 0,
  "compaction" : {
    "lastCompactionRemovedEventCount" : 0,
    "lastCompactionSucceedTimestamp" : 0,
    "lastCompactionFailedTimestamp" : 0,
    "lastCompactionDurationTimeInMills" : 0
  },
  "ownerBroker" : "pulsar:8080"
}
```

Next, we can examine the message contents on the target topic using the following command, which will consume the next 10
messages published to the topic.

```
./bin/pulsar-client consume -n 10 -s sub  persistent://feeds/realtime/ticker-features-partition-0

 Subscribed to topic on pulsar/172.18.0.4:6650 -- consumer: 0
----- got message -----
key:[null], properties:[], content:{"sequence":62153388303,"product_id":"ETH-USD","price":3683.22,"open_24h":3678.46,"volume_24h":27453.285,"low_24h":3658.0,"high_24h":3707.99,"volume_30d":2682576.4227785,"best_bid":3683.22,"best_bid_size":1.6689007,"best_ask":3683.23,"best_ask_size":0.6517148,"side":"sell","latest_emwa":0.14638801,"latest_std":2.6572865E-5,"latest_variance":7.061171E-10,"rolling_mean":0.1464045,"rolling_std":2.3050288E-5,"rolling_variance":5.3131577E-10}
----- got message -----
key:[null], properties:[], content:{"sequence":62153388305,"product_id":"ETH-USD","price":3683.22,"open_24h":3678.46,"volume_24h":27453.725,"low_24h":3658.0,"high_24h":3707.99,"volume_30d":2682576.8618785,"best_bid":3683.22,"best_bid_size":1.2298007,"best_ask":3683.23,"best_ask_size":0.6517148,"side":"sell","latest_emwa":0.14638801,"latest_std":2.6572865E-5,"latest_variance":7.061171E-10,"rolling_mean":0.1464045,"rolling_std":2.3050288E-5,"rolling_variance":5.3131577E-10}
----- got message -----
key:[null], properties:[], content:{"sequence":62153388429,"product_id":"ETH-USD","price":3683.23,"open_24h":3678.46,"volume_24h":27453.725,"low_24h":3658.0,"high_24h":3707.99,"volume_30d":2682576.86266141,"best_bid":3683.22,"best_bid_size":1.2298007,"best_ask":3683.23,"best_ask_size":0.6509319,"side":"buy","latest_emwa":0.14638801,"latest_std":2.6572865E-5,"latest_variance":7.061171E-10,"rolling_mean":0.1464045,"rolling_std":2.3050288E-5,"rolling_variance":5.3131577E-10}
----- got message -----
key:[null], properties:[], content:{"sequence":12992389853,"product_id":"SOL-USD","price":158.74,"open_24h":162.29,"volume_24h":410477.78,"low_24h":156.51,"high_24h":163.72,"volume_30d":2.409904754920079E7,"best_bid":158.73,"best_bid_size":76.316895,"best_ask":158.74,"best_ask_size":0.51449215,"side":"buy","latest_emwa":0.14638801,"latest_std":2.6572865E-5,"latest_variance":7.061171E-10,"rolling_mean":0.1464045,"rolling_std":2.3050288E-5,"rolling_variance":5.3131577E-10}
----- got message -----
key:[null], properties:[], content:{"sequence":62153388560,"product_id":"ETH-USD","price":3683.23,"open_24h":3678.46,"volume_24h":27453.727,"low_24h":3658.0,"high_24h":3707.99,"volume_30d":2682576.86408331,"best_bid":3683.22,"best_bid_size":1.1486504,"best_ask":3683.23,"best_ask_size":0.64951,"side":"buy","latest_emwa":0.14638801,"latest_std":2.6572865E-5,"latest_variance":7.061171E-10,"rolling_mean":0.1464045,"rolling_std":2.3050288E-5,"rolling_variance":5.3131577E-10}
----- got message -----
key:[null], properties:[], content:{"sequence":1292510842,"product_id":"USDT-USD","price":0.99991,"open_24h":0.99957,"volume_24h":7.146508E7,"low_24h":0.99953,"high_24h":0.99997,"volume_30d":4.57299470313E9,"best_bid":0.99991,"best_bid_size":827595.7,"best_ask":0.99992,"best_ask_size":332620.12,"side":"sell","latest_emwa":0.14638801,"latest_std":2.6572865E-5,"latest_variance":7.061171E-10,"rolling_mean":0.1464045,"rolling_std":2.3050288E-5,"rolling_variance":5.3131577E-10}
----- got message -----
key:[null], properties:[], content:{"sequence":1292510845,"product_id":"USDT-USD","price":0.99991,"open_24h":0.99957,"volume_24h":7.1465224E7,"low_24h":0.99953,"high_24h":0.99997,"volume_30d":4.57299484149E9,"best_bid":0.99991,"best_bid_size":827457.3,"best_ask":0.99992,"best_ask_size":332620.12,"side":"sell","latest_emwa":0.14638801,"latest_std":2.6572865E-5,"latest_variance":7.061171E-10,"rolling_mean":0.1464045,"rolling_std":2.3050288E-5,"rolling_variance":5.3131577E-10}
----- got message -----
key:[null], properties:[], content:{"sequence":1292510850,"product_id":"USDT-USD","price":0.99991,"open_24h":0.99957,"volume_24h":7.1466032E7,"low_24h":0.99953,"high_24h":0.99997,"volume_30d":4.57299565469E9,"best_bid":0.99991,"best_bid_size":826644.1,"best_ask":0.99992,"best_ask_size":332620.12,"side":"sell","latest_emwa":0.14638801,"latest_std":2.6572865E-5,"latest_variance":7.061171E-10,"rolling_mean":0.1464045,"rolling_std":2.3050288E-5,"rolling_variance":5.3131577E-10}
----- got message -----
key:[null], properties:[], content:{"sequence":62153388625,"product_id":"ETH-USD","price":3683.23,"open_24h":3678.46,"volume_24h":27453.73,"low_24h":3658.0,"high_24h":3707.99,"volume_30d":2682576.86769706,"best_bid":3683.22,"best_bid_size":1.2301008,"best_ask":3683.23,"best_ask_size":0.64589626,"side":"buy","latest_emwa":0.14638801,"latest_std":2.6572865E-5,"latest_variance":7.061171E-10,"rolling_mean":0.1464045,"rolling_std":2.3050288E-5,"rolling_variance":5.3131577E-10}
----- got message -----
key:[null], properties:[], content:{"sequence":62153388627,"product_id":"ETH-USD","price":3683.23,"open_24h":3678.46,"volume_24h":27453.732,"low_24h":3658.0,"high_24h":3707.99,"volume_30d":2682576.86959657,"best_bid":3683.22,"best_bid_size":1.2301008,"best_ask":3683.23,"best_ask_size":0.6439968,"side":"buy","latest_emwa":0.14638801,"latest_std":2.6572865E-5,"latest_variance":7.061171E-10,"rolling_mean":0.1464045,"rolling_std":2.3050288E-5,"rolling_variance":5.3131577E-10}
```

If everything is working properly, you should see output similar to that shown above. This indicates that your 
materialized view is indeed working as expected.

