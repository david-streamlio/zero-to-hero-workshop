#!/bin/bash

docker exec -it pulsar-broker sh -c \
  "./bin/pulsar-admin functions create \
     --jar /etc/pulsar-functions/lib/coinbase-ticker-sink-1.0.0.nar \
     --function-config-file /etc/pulsar-functions/conf/coinbase-ticker-tracker.yaml"


sleep 5

docker exec -it pulsar-broker sh -c \
  "./bin/pulsar-admin functions create \
     --jar /etc/pulsar-functions/lib/coinbase-ticker-sink-1.0.0.nar \
     --function-config-file /etc/pulsar-functions/conf/coinbase-ticker-history.yaml"