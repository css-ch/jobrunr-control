#!/bin/bash

# Example script to trigger a job and monitor its status
# Replace JOB_ID with an actual scheduled job ID

set -e

# Configuration
BASE_URL="http://localhost:8080"
JOB_ID="${1:-}"

if [ -z "$JOB_ID" ]; then
    echo "Usage: $0 <job-id>"
    echo ""
    echo "Example: $0 123e4567-e89b-12d3-a456-426614174000"
    echo ""
    echo "To find a job ID, first list scheduled jobs:"
    echo "  curl $BASE_URL/api/jobs/scheduled | jq"
    exit 1
fi

echo "========================================="
echo "External Trigger API Example"
echo "========================================="
echo ""

# Step 1: Trigger the job
echo "Step 1: Triggering job $JOB_ID..."
TRIGGER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/external-trigger/$JOB_ID/trigger")
echo "Response: $TRIGGER_RESPONSE"
echo ""

# Check if trigger was successful
if echo "$TRIGGER_RESPONSE" | grep -q "successfully"; then
    echo "✓ Job triggered successfully!"
else
    echo "✗ Failed to trigger job"
    exit 1
fi

echo ""
echo "Step 2: Monitoring job status..."
echo ""

# Step 2: Poll for status
MAX_ATTEMPTS=60
ATTEMPT=0
SLEEP_INTERVAL=2

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    ATTEMPT=$((ATTEMPT + 1))

    # Get job status
    STATUS_RESPONSE=$(curl -s "$BASE_URL/api/external-trigger/$JOB_ID/status")

    # Extract status (requires jq)
    if command -v jq &> /dev/null; then
        JOB_STATUS=$(echo "$STATUS_RESPONSE" | jq -r '.status')
        JOB_NAME=$(echo "$STATUS_RESPONSE" | jq -r '.jobName')
        JOB_TYPE=$(echo "$STATUS_RESPONSE" | jq -r '.jobType')

        echo "[$ATTEMPT] Job: $JOB_NAME ($JOB_TYPE) - Status: $JOB_STATUS"

        # Check if it's a batch job
        BATCH_PROGRESS=$(echo "$STATUS_RESPONSE" | jq -r '.batchProgress')
        if [ "$BATCH_PROGRESS" != "null" ]; then
            TOTAL=$(echo "$STATUS_RESPONSE" | jq -r '.batchProgress.total')
            SUCCEEDED=$(echo "$STATUS_RESPONSE" | jq -r '.batchProgress.succeeded')
            FAILED=$(echo "$STATUS_RESPONSE" | jq -r '.batchProgress.failed')
            PENDING=$(echo "$STATUS_RESPONSE" | jq -r '.batchProgress.pending')
            PROGRESS=$(echo "$STATUS_RESPONSE" | jq -r '.batchProgress.progress')

            echo "    Batch Progress: $SUCCEEDED succeeded, $FAILED failed, $PENDING pending (${PROGRESS}%)"
        fi

        # Check if job is complete
        if [ "$JOB_STATUS" = "SUCCEEDED" ] || [ "$JOB_STATUS" = "FAILED" ]; then
            echo ""
            echo "========================================="
            echo "Job finished with status: $JOB_STATUS"
            echo "========================================="
            echo ""
            echo "Full response:"
            echo "$STATUS_RESPONSE" | jq .

            if [ "$JOB_STATUS" = "SUCCEEDED" ]; then
                exit 0
            else
                exit 1
            fi
        fi
    else
        echo "[$ATTEMPT] Status response: $STATUS_RESPONSE"
        echo "(Install 'jq' for formatted output)"

        # Simple check without jq
        if echo "$STATUS_RESPONSE" | grep -q '"status":"SUCCEEDED"'; then
            echo ""
            echo "✓ Job completed successfully!"
            exit 0
        elif echo "$STATUS_RESPONSE" | grep -q '"status":"FAILED"'; then
            echo ""
            echo "✗ Job failed!"
            exit 1
        fi
    fi

    sleep $SLEEP_INTERVAL
done

echo ""
echo "⚠ Timeout: Job did not complete within expected time"
echo "Last status: $STATUS_RESPONSE"
exit 2
