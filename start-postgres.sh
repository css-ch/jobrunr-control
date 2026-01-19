#!/bin/bash

podman run -d \
 --name jobrunr-db \
 -e POSTGRES_PASSWORD=your_strong_password \
 -p 5432:5432 \
 postgres:17