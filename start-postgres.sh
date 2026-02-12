#!/bin/bash

CONTAINER_NAME="jobrunr-db"
POSTGRES_USER="postgres"
POSTGRES_PASSWORD="your_strong_password"
POSTGRES_DB="postgres"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Determine container engine: prefer podman, fallback to docker
if command -v podman &> /dev/null; then
    CONTAINER_ENGINE="podman"
elif command -v docker &> /dev/null; then
    CONTAINER_ENGINE="docker"
else
    echo "Neither podman nor docker is installed. Please install one to continue."
    exit 1
fi

# Remove existing container (stop first if running)
if $CONTAINER_ENGINE container inspect $CONTAINER_NAME &> /dev/null; then
    echo "Removing existing container '$CONTAINER_NAME'..."
    $CONTAINER_ENGINE stop $CONTAINER_NAME &> /dev/null
    $CONTAINER_ENGINE rm $CONTAINER_NAME
fi

echo "Creating and starting new container '$CONTAINER_NAME'..."
$CONTAINER_ENGINE run -d \
    --name $CONTAINER_NAME \
    -p 5432:5432 \
    -e POSTGRES_PASSWORD=$POSTGRES_PASSWORD \
    postgres:15

echo "Waiting for PostgreSQL to be ready..."
until $CONTAINER_ENGINE exec $CONTAINER_NAME pg_isready -U $POSTGRES_USER >/dev/null 2>&1; do
    printf "."
    sleep 1
done
echo ""
echo "✅ PostgreSQL is ready!"

echo "Creating jobrunr_control_parameter_sets table..."
$CONTAINER_ENGINE exec -i $CONTAINER_NAME psql -U $POSTGRES_USER -d $POSTGRES_DB < "$SCRIPT_DIR/docs/sql/postgresql.sql"
echo "✅ Table setup complete."

echo ""
echo "📋 Connection Details:"
echo "   JDBC URL: jdbc:postgresql://localhost:5432/$POSTGRES_DB"
echo "   Username: $POSTGRES_USER"
echo "   Password: $POSTGRES_PASSWORD"
echo ""
echo "🚀 Start dev with PostgreSQL:"
echo "   ./mvnw -f jobrunr-control-example/pom.xml quarkus:dev -Dquarkus.profile=dev,postgres"
echo ""
echo "🛑 To stop:  $CONTAINER_ENGINE stop $CONTAINER_NAME"
echo "🗑️  To remove: $CONTAINER_ENGINE stop $CONTAINER_NAME && $CONTAINER_ENGINE rm $CONTAINER_NAME"
