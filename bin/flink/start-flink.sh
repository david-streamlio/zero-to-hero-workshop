#!/bin/bash

INFRA_DIR="deployments/docker/infrastructure/flink"

docker compose --project-name flink --file $INFRA_DIR/cluster.yaml up -d