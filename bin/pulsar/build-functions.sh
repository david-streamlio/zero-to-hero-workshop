#!/bin/bash

FUNCTIONS_DIR="coinbase-functions"
INFRA_DIR="infrastructure/pulsar/functions/lib"

mvn clean install -f $FUNCTIONS_DIR/pom.xml

values=("coinbase-live-feed" "coinbase-websocket-feed-router")

# Loop over each value
for val in "${values[@]}"; do
  cp $FUNCTIONS_DIR/${val}/target/*.nar $INFRA_DIR/
done
