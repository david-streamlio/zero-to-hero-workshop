#!/bin/bash

docker exec -it pulsar-broker sh -c \
  "./bin/pulsar-admin functions create \
     --jar /etc/pulsar-functions/lib/coinbase-websocket-feed-router-1.0.0.nar \
     --function-config-file /etc/pulsar-functions/conf/coinbase-router.yaml"