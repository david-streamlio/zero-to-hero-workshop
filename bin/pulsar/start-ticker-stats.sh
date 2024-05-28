#!/bin/bash

docker exec -it pulsar-broker sh -c \
  "./bin/pulsar-admin functions create \
     --name StatsFunction \
     --py /etc/pulsar-functions/lib/coinbase-ticker-stats.zip \
     --inputs persistent://feeds/realtime/coinbase-ticker \
     --output persistent://feeds/realtime/coinbase-ticker-stats \
     --tenant feeds \
     --namespace realtime \
     --classname stats.TickerStatsFunction"