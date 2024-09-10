#!/bin/bash

docker compose --project-name pulsar down --volumes

docker compose --project-name mysql down --volumes

docker compose --project-name grafana down --volumes

# docker compose --project-name flink down --volumes

docker network rm real-time-crypto
