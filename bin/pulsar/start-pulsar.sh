#!/bin/bash

INFRA_DIR="deployments/docker/infrastructure/pulsar"

docker compose --project-name pulsar --file $INFRA_DIR/cluster.yaml up -d