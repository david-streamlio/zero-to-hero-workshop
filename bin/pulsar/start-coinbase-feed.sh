#!/bin/bash

docker exec -it pulsar-broker sh -c \
  "./bin/pulsar-admin tenants create feeds"

sleep 5

docker exec -it pulsar-broker sh -c \
  "./bin/pulsar-admin namespaces create feeds/realtime"

docker exec -it pulsar-broker sh -c \
  "./bin/pulsar-admin topics create-partitioned-topic -p 1 persistent://feeds/realtime/ticker-features"

sleep 5

docker exec -it pulsar-broker sh -c \
  "./bin/pulsar-admin sources create \
     --archive /etc/pulsar-functions/lib/coinbase-live-feed-1.0.0.nar \
     --source-config-file /etc/pulsar-functions/conf/coinbase-feed.yaml"