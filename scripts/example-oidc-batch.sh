#!/bin/bash

# Example: OIDC-Authenticated Batch Job Script
# This script demonstrates how to use OIDC authentication with start-and-poll-job.sh
#
# NOTE: This example provides default OIDC configuration for local development.
#       The main start-and-poll-job.sh script requires explicit OIDC configuration.
#
# Prerequisites:
#   1. Start Keycloak: ./start-keycloak.sh
#   2. Start JobRunr Control: ./mvnw -f jobrunr-control-example/pom.xml quarkus:dev -Dquarkus.profile=dev,keycloak

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JOB_SCRIPT="$SCRIPT_DIR/start-and-poll-job.sh"

# OIDC Configuration - Local development defaults for external Keycloak
# These defaults match the configuration in start-keycloak.sh and keycloak-realm.json
: ${JOBRUNR_CONTROL_URL:="http://localhost:9090"}
: ${OIDC_CLIENT_ID:="jobrunr-control"}
: ${OIDC_CLIENT_SECRET:="jobrunr-secret"}
: ${OIDC_SERVER:="http://localhost:8080"}
: ${OIDC_REALM:="jobrunr"}

# Job Configuration
TEMPLATE_ID="${BATCH_TEMPLATE_ID:-019c8283-9b36-7d63-ac63-0695a2c2acdf}"
POSTFIX="batch-$(date +%Y%m%d-%H%M%S)"

# Job Parameters
ENVIRONMENT="${ENV:-development}"
BATCH_SIZE="${BATCH_SIZE:-100}"
DRY_RUN="${DRY_RUN:-false}"
RETRY_COUNT="${RETRY_COUNT:-3}"

echo "=========================================="
echo "OIDC-Authenticated Batch Job - $(date)"
echo "=========================================="
echo "JobRunr Control: $JOBRUNR_CONTROL_URL"
echo "OIDC Server: $OIDC_SERVER"
echo "OIDC Client: $OIDC_CLIENT_ID"
echo "OIDC Realm: $OIDC_REALM"
echo "=========================================="
echo "Template ID: $TEMPLATE_ID"
echo "Postfix: $POSTFIX"
echo "Environment: $ENVIRONMENT"
echo "Batch Size: $BATCH_SIZE"
echo "Dry Run: $DRY_RUN"
echo "Retry Count: $RETRY_COUNT"
echo "=========================================="
echo ""

# Export OIDC configuration
export JOBRUNR_CONTROL_URL
export OIDC_CLIENT_ID
export OIDC_CLIENT_SECRET
export OIDC_SERVER
export OIDC_REALM

# Start the job and poll for completion
"$JOB_SCRIPT" \
  "$TEMPLATE_ID" \
  "$POSTFIX" \
  "environment=$ENVIRONMENT" \
  "batchSize=$BATCH_SIZE" \
  "dryRun=$DRY_RUN" \
  "retryCount=$RETRY_COUNT"

EXIT_CODE=$?

echo ""
echo "=========================================="
if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ Batch job completed successfully!"
    echo "Job Name: $POSTFIX"
    echo "Exit Code: $EXIT_CODE"

    # Add your post-processing logic here
    # For example: send success notification, trigger next job, etc.

else
    echo "❌ Batch job failed!"
    echo "Exit Code: $EXIT_CODE"

    case $EXIT_CODE in
        1)
            echo "Reason: Job failed or API error"
            ;;
        2)
            echo "Reason: Job was deleted"
            ;;
        3)
            echo "Reason: Timeout reached"
            ;;
        *)
            echo "Reason: Unknown error"
            ;;
    esac

    # Add your error handling logic here
    # For example: send alert, create incident ticket, retry logic, etc.

fi
echo "=========================================="

exit $EXIT_CODE

