#!/bin/bash

CONTAINER_NAME="oracle-db"
PORT=1521
ORACLE_PASSWORD="YourStrongPassword123"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Determine container engine: prefer podman, fallback to docker
if command -v podman &> /dev/null; then
    CONTAINER_ENGINE="podman"
elif command -v docker &> /dev/null; then
    CONTAINER_ENGINE="docker"
else
    echo "Neither podman nor docker is installed."
    exit 1
fi

# Remove existing container (stop first if running)
if $CONTAINER_ENGINE container inspect $CONTAINER_NAME &> /dev/null; then
    echo "Removing existing container '$CONTAINER_NAME'..."
    $CONTAINER_ENGINE stop $CONTAINER_NAME &> /dev/null
    $CONTAINER_ENGINE rm $CONTAINER_NAME
fi

echo "Creating and starting new Oracle container..."
$CONTAINER_ENGINE run -d \
    --name $CONTAINER_NAME \
    -p $PORT:1521 \
    -e ORACLE_PWD=$ORACLE_PASSWORD \
    container-registry.oracle.com/database/free:latest

echo "Waiting for Oracle to be ready (this may take 2-5 minutes on first start)..."
sleep 10

MAX_WAIT=300
COUNTER=0
until $CONTAINER_ENGINE exec $CONTAINER_NAME sh -c 'echo "SELECT 1 FROM DUAL;" | sqlplus -s system/$ORACLE_PWD@//localhost:1521/FREEPDB1' | grep -q "1" 2>/dev/null; do
    printf "."
    sleep 5
    COUNTER=$((COUNTER+5))
    if [ $COUNTER -ge $MAX_WAIT ]; then
        echo ""
        echo "❌ Oracle startup timeout. Check logs with: $CONTAINER_ENGINE logs $CONTAINER_NAME"
        exit 1
    fi
done
echo ""
echo "✅ Oracle is ready!"

echo "Creating JOBRUNR_DATA tablespace..."
$CONTAINER_ENGINE exec $CONTAINER_NAME sh -c "echo \"
CREATE TABLESPACE jobrunr_data
  DATAFILE 'jobrunr_data.dbf' SIZE 100M AUTOEXTEND ON NEXT 10M MAXSIZE UNLIMITED
  SEGMENT SPACE MANAGEMENT AUTO
  ONLINE;
ALTER USER system DEFAULT TABLESPACE jobrunr_data;
GRANT UNLIMITED TABLESPACE TO system;
\" | sqlplus -s system/\$ORACLE_PWD@//localhost:1521/FREEPDB1" 2>/dev/null

echo "Creating jobrunr_control_parameter_sets table..."
$CONTAINER_ENGINE exec -i $CONTAINER_NAME sh -c "sqlplus -s system/\$ORACLE_PWD@//localhost:1521/FREEPDB1" < "$SCRIPT_DIR/docs/sql/oracle.sql"
echo "✅ Table setup complete."

echo ""
echo "📋 Connection Details:"
echo "   JDBC URL: jdbc:oracle:thin:@localhost:$PORT/FREEPDB1"
echo "   Username: system"
echo "   Password: $ORACLE_PASSWORD"
echo "   Default Tablespace: JOBRUNR_DATA"
echo ""
echo "🚀 Start dev with Oracle:"
echo "   ./mvnw -f jobrunr-control-example/pom.xml quarkus:dev -Dquarkus.profile=dev,oracle"
echo ""
echo "🛑 To stop:  $CONTAINER_ENGINE stop $CONTAINER_NAME"
echo "🗑️  To remove: $CONTAINER_ENGINE stop $CONTAINER_NAME && $CONTAINER_ENGINE rm $CONTAINER_NAME"
