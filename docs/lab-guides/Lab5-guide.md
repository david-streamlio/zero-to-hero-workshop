# Hands-On Lab #5: Running Flink

Prerequisites
------------

- [Lab1](Lab1-guide.md)
- [Lab2](Lab2-guide.md)
- [Lab3](Lab3-guide.md)
- [Lab4](Lab4-guide.md)


Stream Processing Infrastructure
--------------------------------------

Before jumping into any of the scenarios, you must start the stream processing platform, which for this workshop will be
Apache Flink.


### 1️⃣ Start Apache Flink

[Apache Flink](https://flink.apache.org/) is a framework and distributed processing engine for stateful computations over unbounded and bounded data
streams. It will serve as our distributed streaming computation platform for this demo. You can start it using the following
command:

```bash
sh ./bin/flink/start-flink.sh
```

Next, you must wait until the Flink containers are all running.....

```
docker ps
CONTAINER ID   IMAGE          COMMAND                  CREATED         STATUS         PORTS                              NAMES
1c010948d856   flink:latest   "/docker-entrypoint.…"   6 seconds ago   Up 6 seconds   6123/tcp, 8081/tcp                 flink-taskmanager-2
bef31746866c   flink:latest   "/docker-entrypoint.…"   6 seconds ago   Up 6 seconds   6123/tcp, 8081/tcp                 flink-taskmanager-1
dca492aaa5ef   flink:latest   "/docker-entrypoint.…"   6 seconds ago   Up 6 seconds   6123/tcp, 0.0.0.0:8081->8081/tcp   flink-jobmanager-1
```


### 2️⃣ Validate Flink is working

At this point, the Apache Flink Web UI is now available at http://localhost:8081

![Flink-UI.png](..%2Fimages%2FFlink-UI.png)

### Explore the Flink UI 

Examine the JobManager Memory Allocation

Examine the Task Manager Memory Allocation
