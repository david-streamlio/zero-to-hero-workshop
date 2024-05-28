#!/bin/bash

FLINK_DIR="coinbase-flink"
INFRA_DIR="infrastructure/flink/jobs"

mvn clean install -f $FLINK_DIR/pom.xml

values=("coinbase-aggregations" "coinbase-filters" "coinbase-joins")

# Move the artifacts to the mount point and delete the target dir
for val in "${values[@]}"; do
  cp $FLINK_DIR/${val}/target/*.jar $INFRA_DIR/
  rm -rf $FLINK_DIR/${val}/target
done

# Cleanup the unwanted jar
rm -rf $INFRA_DIR/*-0.0.1.jar



