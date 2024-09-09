#!/bin/bash

docker compose --project-name pulsar down

docker compose --project-name mysql down

docker compose --project-name grafana down

# docker compose --project-name flink down