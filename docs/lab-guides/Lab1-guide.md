# Hands-On Lab #1: Streaming Platform Infrastructure


### 1️⃣ Start Pulsar

[Apache Pulsar](https://pulsar.apache.org/) is an open-source, distributed messaging and streaming platform built for
the cloud. It will serve as our event streaming storage platform for this workshop.

```bash
sh ./bin/pulsar/start-pulsar.sh
```
An Apache Pulsar cluster comprises three separate layers as shown in the following diagram. A serving layer consisting 
of message brokers, a storage layer consisting of storage nodes known as "bookies", and a metadata layer, which in our 
scenario will be a single Zookeeper node.

![Pulsar-architecture.png](..%2Fimages%2FPulsar-architecture.png)

You can verify that the deployment was successful using the following command, and seeing output similar to that
shown here. This indicates that all three Pulsar-related containers are running.

````bash
docker ps

CONTAINER ID   IMAGE                           COMMAND                  CREATED         STATUS                   PORTS                                            NAMES
d2d2f1371eaa   apachepulsar/pulsar-all:3.2.2   "bash -c 'bin/apply-…"   5 minutes ago   Up 4 minutes (healthy)   0.0.0.0:6650->6650/tcp, 0.0.0.0:8080->8080/tcp   pulsar-broker
0c1afb9a8736   apachepulsar/pulsar-all:3.2.2   "bash -c 'bin/apply-…"   5 minutes ago   Up 4 minutes (healthy)                                                    pulsar-bookie-1
efb296a5fea9   apachepulsar/pulsar-all:3.2.2   "bash -c 'bin/apply-…"   5 minutes ago   Up 5 minutes (healthy)                                                    zookeeper
````

### 2️⃣ Validate Pulsar is working

Now that we have confirmed that all requisite components are running, you can begin to familiarize yourself with Pulsar 
using its command-line tools. Let's start with the `pulsar-admin` tool, which as the name implies, allows us to perform
administrative tasks, such as explore the available tenants, namespaces, and topics in the cluster.

![Pulsar-logical-arch.png](..%2Fimages%2FPulsar-logical-arch.png)

Running the following series of commands to create a new topic and confirm its existence:

```
./bin/pulsar-admin topics create public/default/test-topic

./bin/pulsar-admin topics list  public/default
persistent://public/default/test-topic
```

First, let's start a consumer on the topic we just created. For this, you will need to use the `pulsar-client` command 
line tool as shown below:

```
./bin/pulsar-client consume -n 3 --subscription-name my-sub  persistent://public/default/test-topic

...
Subscribed to topic on pulsar/172.23.0.4:6650 -- consumer: 0
```

Next, open a second shell on the broker and publish some data to the topic. 
```
produce -n 3 -m "Zero to Hero" persistent://public/default/test-topic

...
INFO  org.apache.pulsar.client.cli.PulsarClientTool - 3 messages successfully produced
```

In the consumer window you should see the messages getting consumed.

```
----- got message -----
key:[null], properties:[], content:Zero to Hero
----- got message -----
key:[null], properties:[], content:Zero to Hero
----- got message -----
key:[null], properties:[], content:Zero to Hero
```

We can then use the `pulsar-admin` tool again to display the statistics on the topic as well.

```
./bin/pulsar-admin topics stats persistent://public/default/test-topic
{
  "msgRateIn" : 0.0,
  "msgThroughputIn" : 0.0,
  "msgRateOut" : 0.0,
  "msgThroughputOut" : 0.0,
  "bytesInCounter" : 318,
  "msgInCounter" : 6,
  "bytesOutCounter" : 183,
  "msgOutCounter" : 3,
  "averageMsgSize" : 0.0,
  "msgChunkPublished" : false,
  "storageSize" : 366,
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
  "publishers" : [ ],
  "waitingPublishers" : 0,
  "subscriptions" : {
    "my-sub" : {
      "msgRateOut" : 0.0,
      "msgThroughputOut" : 0.0,
      "bytesOutCounter" : 183,
      "msgOutCounter" : 3,
      "msgRateRedeliver" : 0.0,
      "messageAckRate" : 0.0,
      "chunkedMessageRate" : 0,
      "msgBacklog" : 0,
      "backlogSize" : 0,
      "earliestMsgPublishTimeInBacklog" : 0,
      "msgBacklogNoDelayed" : 0,
      "blockedSubscriptionOnUnackedMsgs" : false,
      "msgDelayed" : 0,
      "unackedMessages" : 0,
      "type" : "Exclusive",
      "msgRateExpired" : 0.0,
      "totalMsgExpired" : 0,
      "lastExpireTimestamp" : 0,
      "lastConsumedFlowTimestamp" : 1717040786749,
      "lastConsumedTimestamp" : 0,
      "lastAckedTimestamp" : 0,
      "lastMarkDeleteAdvancedTimestamp" : 1717040791809,
      "consumers" : [ ],
      "isDurable" : true,
      "isReplicated" : false,
      "allowOutOfOrderDelivery" : false,
      "consumersAfterMarkDeletePosition" : { },
      "nonContiguousDeletedMessagesRanges" : 0,
      "nonContiguousDeletedMessagesRangesSerializedSize" : 0,
      "delayedMessageIndexSizeInBytes" : 0,
      "subscriptionProperties" : { },
      "filterProcessedMsgCount" : 0,
      "filterAcceptedMsgCount" : 0,
      "filterRejectedMsgCount" : 0,
      "filterRescheduledMsgCount" : 0,
      "durable" : true,
      "replicated" : false
    }
  },
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

