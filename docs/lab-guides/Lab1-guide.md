# Hands-On Lab #1: Streaming Platform Infrastructure


### 1️⃣ Start Pulsar

[Apache Pulsar](https://pulsar.apache.org/) is an open-source, distributed messaging and streaming platform built for
the cloud. It will serve as our event streaming storage platform for this workshop.

```bash
sh ./bin/pulsar/start-pulsar.sh
```

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

TODO