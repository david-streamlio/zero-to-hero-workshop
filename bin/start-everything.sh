#!/bin/bash

echo "Starting Apache Pulsar..."
sh ./bin/pulsar/start-pulsar.sh

sleep 15

echo "Starting the data feed..."
sh ./bin/pulsar/start-coinbase-feed.sh

sleep 5

echo "Starting the content based routing function..."
sh ./bin/pulsar/start-coinbase-feed-router.sh

sleep 5

echo "Starting the ticker stats function..."
sh ./bin/pulsar/start-ticker-stats.sh

sleep 5

sh ./bin/flink/start-flink.sh
