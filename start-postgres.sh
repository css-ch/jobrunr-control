#!/bin/bash

CONTAINER_NAME="jobrunr-db"

# Determine container engine: prefer podman, fallback to docker
if command -v podman &> /dev/null; then
    CONTAINER_ENGINE="podman"
elif command -v docker &> /dev/null; then
    CONTAINER_ENGINE="docker"
else
    echo "Neither podman nor docker is installed. Please install one to continue."
    exit 1
fi

# Check if the container is already running
if $CONTAINER_ENGINE ps --filter "name=^${CONTAINER_NAME}$" --filter "status=running" --format '{{.Names}}' | grep -wq "$CONTAINER_NAME"; then
    echo "Container '$CONTAINER_NAME' is already running. Nothing to do."
    exit 0
fi

# Check if the container already exists (but not running)
if $CONTAINER_ENGINE container inspect $CONTAINER_NAME &> /dev/null; then
    echo "Container '$CONTAINER_NAME' already exists. Starting it..."
    $CONTAINER_ENGINE start $CONTAINER_NAME
else
    echo "Creating and starting new container '$CONTAINER_NAME'..."
    $CONTAINER_ENGINE run -d --name $CONTAINER_NAME -p 5432:5432 -e POSTGRES_PASSWORD=your_strong_password postgres:15
fi
