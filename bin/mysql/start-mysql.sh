#!/bin/bash

INFRA_DIR="deployments/docker/infrastructure/mysql"

docker compose --project-name mysql --file $INFRA_DIR/cluster.yaml up -d