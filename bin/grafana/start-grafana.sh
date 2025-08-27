#!/bin/bash

INFRA_DIR="deployments/docker/infrastructure/grafana"

docker compose --project-name grafana --file $INFRA_DIR/cluster.yaml up -d