#!/bin/bash
# Start Keycloak with Podman Compose for JobRunr Control
# This script starts a Keycloak instance with the jobrunr realm pre-configured

set -e

echo "Starting Keycloak for JobRunr Control..."
echo "============================================"

# Check if podman-compose is available
if command -v podman-compose &> /dev/null; then
    COMPOSE_CMD="podman-compose"
    echo "✅ Using podman-compose"
elif command -v podman &> /dev/null && podman compose version &> /dev/null; then
    COMPOSE_CMD="podman compose"
    echo "✅ Using podman compose"
elif command -v docker compose &> /dev/null; then
    COMPOSE_CMD="docker compose"
    echo "⚠️  Using docker compose (podman not found)"
else
    echo "❌ Error: Neither 'podman-compose', 'podman compose', nor 'docker compose' found."
    echo "Please install Podman or Docker Desktop."
    exit 1
fi

# Start Keycloak
$COMPOSE_CMD -f docker-compose-keycloak.yml up -d

echo ""
echo "============================================"
echo "Keycloak started successfully!"
echo "============================================"
echo "Admin Console: http://localhost:8080/admin"
echo "Username: admin"
echo "Password: admin"
echo ""
echo "Realm: jobrunr"
echo "Client ID: jobrunr-control"
echo "Client Secret: jobrunr-secret"
echo ""
echo "Test Users:"
echo "  - admin/admin (all roles)"
echo "  - configurator/configurator (configurator, viewer)"
echo "  - viewer/viewer (viewer only)"
echo "  - guest/guest (no roles)"
echo "============================================"
echo ""
echo "To start the example app with this Keycloak:"
echo "  ./mvnw -f jobrunr-control-example/pom.xml quarkus:dev -Dquarkus.profile=dev,keycloak"
echo ""
echo "To stop Keycloak, run:"
echo "  $COMPOSE_CMD -f docker-compose-keycloak.yml down"
echo ""

