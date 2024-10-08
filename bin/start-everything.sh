#!/bin/bash

echo "Starting Apache Pulsar..."
sh ./bin/pulsar/start-pulsar.sh

sleep 5 && echo "Starting MySQL Database..."
sh ./bin/mysql/start-mysql.sh

sleep 5 && echo "Starting Graphana...."
sh ./bin/grafana/start-grafana.sh

sleep 15 && echo "Starting the data feed..."
sh ./bin/pulsar/start-coinbase-feed.sh

sleep 5 && echo "Starting the content based routing function..."
sh ./bin/pulsar/start-coinbase-feed-router.sh

sleep 5 && echo "Starting ticker sink..."
sh ./bin/pulsar/start-coinbase-ticker-tracker.sh

# sleep 5 && echo "Starting the ticker stats function..."
# sh ./bin/pulsar/start-ticker-stats.sh

# sleep 5 && echo "Starting Flink..."
# sh ./bin/flink/start-flink.sh
