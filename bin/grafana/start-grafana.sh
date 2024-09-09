#!/bin/bash

INFRA_DIR="infrastructure/grafana"

docker compose --project-name grafana --file $INFRA_DIR/cluster.yaml up -d