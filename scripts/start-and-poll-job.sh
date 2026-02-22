#!/bin/bash

# JobRunr Control - Start Template and Poll for Completion
# Usage: ./start-and-poll-job.sh <job-id> <postfix> [key1=value1] [key2=value2] ...

set -e

# Configuration
BASE_URL="${JOBRUNR_CONTROL_URL:-http://localhost:9090}"
API_PATH="/q/jobrunr-control/api/jobs"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL:-5}"
MAX_POLL_ATTEMPTS="${MAX_ATTEMPTS:-720}"  # 1 hour with 5s interval

# Authentication Configuration
USERNAME="${JOBRUNR_USER:-admin}"
PASSWORD="${JOBRUNR_PASSWORD:-admin}"

# OIDC Configuration (no defaults - must be explicitly set)
OIDC_CLIENT_ID="${OIDC_CLIENT_ID:-}"
OIDC_CLIENT_SECRET="${OIDC_CLIENT_SECRET:-}"
OIDC_TOKEN_URL="${OIDC_TOKEN_URL:-}"
OIDC_REALM="${OIDC_REALM:-}"
OIDC_SERVER="${OIDC_SERVER:-}"

# Token management
ACCESS_TOKEN=""
TOKEN_EXPIRY=0
TOKEN_REFRESH_BUFFER=30  # Refresh token 30 seconds before expiry

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Aliases for compatibility
success() {
    log_success "$1"
}

error() {
    log_error "$1"
}

warn() {
    log_warning "$1"
}

log() {
    log_info "$1"
}

# Status termination
# $1: Exit Code, $2: Message
terminate() {
    local code=$1
    local msg=$2
    case $code in
        0) log_success "$msg"; exit 0 ;;
        1) log_error "[SCRIPT FEHLER] $msg"; exit 1 ;;
        2) log_warning "[JOB FEHLER] $msg"; exit 2 ;;
        *) exit "$code" ;;
    esac
}

# OIDC Token Management Functions
get_oidc_token() {
    local token_url="$1"

    log_info "Requesting OIDC access token..."

    local token_response=$(curl -s -w "\n%{http_code}" \
        -X POST "$token_url" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials" \
        -d "client_id=$OIDC_CLIENT_ID" \
        -d "client_secret=$OIDC_CLIENT_SECRET")

    local http_code=$(echo "$token_response" | tail -n1)
    local response_body=$(echo "$token_response" | sed '$d')

    if [ "$http_code" -ne 200 ]; then
        log_error "Failed to obtain OIDC token. HTTP Status: $http_code"
        log_error "Response: $response_body"
        return 1
    fi

    # Extract access token and expiry
    ACCESS_TOKEN=$(echo "$response_body" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
    local expires_in=$(echo "$response_body" | grep -o '"expires_in":[0-9]*' | cut -d':' -f2)

    if [ -z "$ACCESS_TOKEN" ]; then
        log_error "Failed to extract access token from response"
        log_error "Response: $response_body"
        return 1
    fi

    # Calculate token expiry time (current time + expires_in - buffer)
    TOKEN_EXPIRY=$(($(date +%s) + expires_in - TOKEN_REFRESH_BUFFER))

    log_success "OIDC token obtained successfully (expires in ${expires_in}s)"
    return 0
}

refresh_token_if_needed() {
    local current_time=$(date +%s)

    if [ $current_time -ge $TOKEN_EXPIRY ]; then
        log_info "Token expired or about to expire, refreshing..."
        get_oidc_token "$1"
        return $?
    fi

    return 0
}

# Extract JSON value (simple grep-based parser)
get_json_val() {
    local key=$1
    local json=$2
    echo "$json" | grep -o "\"$key\":\"[^\"]*\"" | cut -d'"' -f4 | head -1
}

# Parse command line arguments
# Allow 1 argument if BATCH_TEMPLATE_ID is set, otherwise require 2
MIN_ARGS=2
if [ -n "$BATCH_TEMPLATE_ID" ]; then
    MIN_ARGS=1
fi

if [ $# -lt $MIN_ARGS ]; then
    log_error "Usage: $0 <job-id> <postfix> [key1=value1] [key2=value2] ..."
    log_error "   or: BATCH_TEMPLATE_ID=<uuid> $0 <postfix> [key1=value1] [key2=value2] ..."
    log_error ""
    log_error "Arguments:"
    log_error "  job-id   - UUID of the job template to start"
    log_error "  postfix  - Postfix to append to the job name"
    log_error "  key=val  - Optional job parameters"
    log_error ""
    log_error "Environment Variables (Required):"
    log_error "  BATCH_TEMPLATE_ID    - UUID of the job template (alternative to job-id argument)"
    log_error ""
    log_error "Environment Variables (Optional):"
    log_error "  JOBRUNR_CONTROL_URL  - Base URL (default: http://localhost:9090)"
    log_error ""
    log_error "  Basic Authentication (legacy):"
    log_error "    JOBRUNR_USER       - Username (default: admin)"
    log_error "    JOBRUNR_PASSWORD   - Password (default: admin)"
    log_error ""
    log_error "  OIDC Authentication (recommended):"
    log_error "    OIDC_CLIENT_ID     - OIDC client ID (e.g., jobrunr-control)"
    log_error "    OIDC_CLIENT_SECRET - OIDC client secret (required)"
    log_error "    OIDC_TOKEN_URL     - Token endpoint URL (optional, auto-detected)"
    log_error "    OIDC_SERVER        - OIDC server URL (required if OIDC_TOKEN_URL not set)"
    log_error "    OIDC_REALM         - OIDC realm (required if OIDC_TOKEN_URL not set)"
    log_error ""
    log_error "  Polling Configuration:"
    log_error "    POLL_INTERVAL      - Poll interval in seconds (default: 5)"
    log_error "    MAX_ATTEMPTS       - Maximum poll attempts (default: 720)"
    log_error ""
    log_error "Examples:"
    log_error "  # Using command line argument"
    log_error "  $0 550e8400-e29b-41d4-a716-446655440000 \"batch-\$(date +%Y%m%d)\""
    log_error ""
    log_error "  # Using BATCH_TEMPLATE_ID environment variable"
    log_error "  export BATCH_TEMPLATE_ID=550e8400-e29b-41d4-a716-446655440000"
    log_error "  $0 \"batch-\$(date +%Y%m%d)\""
    log_error ""
    log_error "  # With OIDC authentication"
    log_error "  export OIDC_CLIENT_ID=jobrunr-control"
    log_error "  export OIDC_CLIENT_SECRET=jobrunr-secret"
    log_error "  export BATCH_TEMPLATE_ID=550e8400-e29b-41d4-a716-446655440000"
    log_error "  $0 \"batch-\$(date +%Y%m%d)\" \"env=prod\""
    exit 1
fi

# Get job ID and postfix from arguments or environment variable
if [ -n "$BATCH_TEMPLATE_ID" ] && [ $# -eq 1 ]; then
    # Using BATCH_TEMPLATE_ID with single argument (postfix)
    JOB_ID="$BATCH_TEMPLATE_ID"
    POSTFIX="$1"
    shift 1
elif [ $# -ge 2 ]; then
    # Traditional usage with job-id and postfix as arguments
    JOB_ID="$1"
    POSTFIX="$2"
    shift 2
else
    log_error "Error: Invalid arguments. See usage above."
    exit 1
fi

# Validate that job ID is provided
if [ -z "$JOB_ID" ]; then
    log_error "Error: Job ID must be provided either as first argument or via BATCH_TEMPLATE_ID environment variable"
    log_error ""
    log_error "Usage: $0 <job-id> <postfix> [key1=value1] [key2=value2] ..."
    log_error "   or: BATCH_TEMPLATE_ID=<uuid> $0 <postfix> [key1=value1] [key2=value2] ..."
    exit 1
fi

# Validate that postfix is provided
if [ -z "$POSTFIX" ]; then
    log_error "Error: Postfix is required"
    log_error ""
    log_error "Usage: $0 <job-id> <postfix> [key1=value1] [key2=value1] ..."
    exit 1
fi

# Validate JOB_ID format (basic UUID check)
if ! [[ "$JOB_ID" =~ ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$ ]]; then
    terminate 1 "Invalid job-id format. Expected UUID format (e.g., 550e8400-e29b-41d4-a716-446655440000)"
fi

# Build parameters JSON object
PARAMETERS_JSON="{"
FIRST=true
for arg in "$@"; do
    if [[ "$arg" == *"="* ]]; then
        KEY="${arg%%=*}"
        VALUE="${arg#*=}"

        if [ "$FIRST" = true ]; then
            FIRST=false
        else
            PARAMETERS_JSON+=","
        fi

        # Try to detect type and format accordingly
        if [[ "$VALUE" =~ ^-?[0-9]+$ ]]; then
            # Integer
            PARAMETERS_JSON+="\"$KEY\":$VALUE"
        elif [[ "$VALUE" =~ ^-?[0-9]+\.[0-9]+$ ]]; then
            # Float
            PARAMETERS_JSON+="\"$KEY\":$VALUE"
        elif [[ "$VALUE" == "true" ]] || [[ "$VALUE" == "false" ]]; then
            # Boolean
            PARAMETERS_JSON+="\"$KEY\":$VALUE"
        else
            # String (escape quotes)
            ESCAPED_VALUE="${VALUE//\"/\\\"}"
            PARAMETERS_JSON+="\"$KEY\":\"$ESCAPED_VALUE\""
        fi
    else
        log_warning "Ignoring invalid parameter format: $arg (expected key=value)"
    fi
done
PARAMETERS_JSON+="}"

# Build request body
REQUEST_BODY=$(cat <<EOF
{
  "postfix": "$POSTFIX",
  "parameters": $PARAMETERS_JSON
}
EOF
)

log_info "Starting job template: $JOB_ID"
log_info "Postfix: $POSTFIX"
log_info "Parameters: $PARAMETERS_JSON"
log_info ""

# Initialize authentication
if [ -n "$OIDC_CLIENT_ID" ] && [ -n "$OIDC_CLIENT_SECRET" ]; then
    log_info "Using OIDC authentication (client: $OIDC_CLIENT_ID)"

    # Auto-detect token URL if not provided
    if [ -z "$OIDC_TOKEN_URL" ]; then
        # Validate required variables for auto-detection
        if [ -z "$OIDC_SERVER" ] || [ -z "$OIDC_REALM" ]; then
            terminate 1 "OIDC_SERVER and OIDC_REALM are required when OIDC_TOKEN_URL is not set"
        fi
        OIDC_TOKEN_URL="$OIDC_SERVER/realms/$OIDC_REALM/protocol/openid-connect/token"
        log_info "Using auto-detected token URL: $OIDC_TOKEN_URL"
    fi

    # Get initial token
    if ! get_oidc_token "$OIDC_TOKEN_URL"; then
        terminate 1 "Authentifizierung fehlgeschlagen."
    fi
else
    log_info "Using basic authentication (user: $USERNAME)"
fi
log_info ""

# Start the job
START_URL="$BASE_URL$API_PATH/$JOB_ID/start"
log_info "POST $START_URL"

if [ -n "$OIDC_CLIENT_ID" ] && [ -n "$OIDC_CLIENT_SECRET" ]; then
    START_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -X POST "$START_URL" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$REQUEST_BODY")
else
    START_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -X POST "$START_URL" \
        -u "$USERNAME:$PASSWORD" \
        -H "Content-Type: application/json" \
        -d "$REQUEST_BODY")
fi

HTTP_CODE=$(echo "$START_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$START_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -ne 200 ]; then
    terminate 1 "API nicht erreichbar oder Request fehlerhaft. HTTP Status: $HTTP_CODE"
fi

# Extract the actual job ID from response
ACTUAL_JOB_ID=$(get_json_val "jobId" "$RESPONSE_BODY")
MESSAGE=$(get_json_val "message" "$RESPONSE_BODY")

if [ -z "$ACTUAL_JOB_ID" ]; then
    terminate 1 "JobId konnte nicht extrahiert werden."
fi

log_success "Job started successfully!"
log_info "Job ID: $ACTUAL_JOB_ID"
log_info "Message: $MESSAGE"
log_info ""

# Poll for completion
log_info "Polling for job completion (interval: ${POLL_INTERVAL_SECONDS}s, max attempts: ${MAX_POLL_ATTEMPTS})..."
log_info ""

STATUS_URL="$BASE_URL$API_PATH/$ACTUAL_JOB_ID"
ATTEMPT=0
LAST_STATUS=""
LAST_PROGRESS=""

while [ $ATTEMPT -lt $MAX_POLL_ATTEMPTS ]; do
    ATTEMPT=$((ATTEMPT + 1))

    # Refresh OIDC token if needed
    if [ -n "$OIDC_CLIENT_ID" ] && [ -n "$OIDC_CLIENT_SECRET" ]; then
        if ! refresh_token_if_needed "$OIDC_TOKEN_URL"; then
            log_warning "Failed to refresh OIDC token, will retry..."
        fi
    fi

    # Get job status
    if [ -n "$OIDC_CLIENT_ID" ] && [ -n "$OIDC_CLIENT_SECRET" ]; then
        STATUS_RESPONSE=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer $ACCESS_TOKEN" \
            -H "Accept: application/json" \
            "$STATUS_URL")
    else
        STATUS_RESPONSE=$(curl -s -w "\n%{http_code}" \
            -u "$USERNAME:$PASSWORD" \
            -H "Accept: application/json" \
            "$STATUS_URL")
    fi

    HTTP_CODE=$(echo "$STATUS_RESPONSE" | tail -n1)
    STATUS_BODY=$(echo "$STATUS_RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" -ne 200 ]; then
        log_warning "Verbindung verloren, versuche erneut... (Attempt $ATTEMPT/$MAX_POLL_ATTEMPTS)"
        sleep "$POLL_INTERVAL_SECONDS"
        continue
    fi

    # Extract status information
    STATUS=$(get_json_val "status" "$STATUS_BODY")
    STARTED_AT=$(get_json_val "startedAt" "$STATUS_BODY")
    FINISHED_AT=$(get_json_val "finishedAt" "$STATUS_BODY")

    # Extract batch progress if available
    PROGRESS_TOTAL=$(echo "$STATUS_BODY" | grep -o '"total":[0-9]*' | cut -d':' -f2)
    PROGRESS_SUCCEEDED=$(echo "$STATUS_BODY" | grep -o '"succeeded":[0-9]*' | cut -d':' -f2)
    PROGRESS_FAILED=$(echo "$STATUS_BODY" | grep -o '"failed":[0-9]*' | cut -d':' -f2)
    PROGRESS_PENDING=$(echo "$STATUS_BODY" | grep -o '"pending":[0-9]*' | cut -d':' -f2)
    PROGRESS_PERCENT=$(echo "$STATUS_BODY" | grep -o '"progress":[0-9.]*' | cut -d':' -f2)

    # Build progress string for comparison
    CURRENT_PROGRESS="$STATUS"
    if [ -n "$PROGRESS_TOTAL" ] && [ "$PROGRESS_TOTAL" -gt 0 ]; then
        CURRENT_PROGRESS="$STATUS [$PROGRESS_SUCCEEDED/$PROGRESS_TOTAL succeeded, $PROGRESS_FAILED failed, $PROGRESS_PENDING pending] ${PROGRESS_PERCENT}%"
    fi

    # Only log if status or progress changed
    if [ "$CURRENT_PROGRESS" != "$LAST_PROGRESS" ]; then
        if [ -n "$PROGRESS_TOTAL" ] && [ "$PROGRESS_TOTAL" -gt 0 ]; then
            log_info "[$ATTEMPT] Status: $STATUS | Progress: $PROGRESS_SUCCEEDED/$PROGRESS_TOTAL succeeded, $PROGRESS_FAILED failed, $PROGRESS_PENDING pending (${PROGRESS_PERCENT}%)"
        else
            log_info "[$ATTEMPT] Status: $STATUS"
        fi
        LAST_PROGRESS="$CURRENT_PROGRESS"
    fi

    # Check for terminal states
    case "$STATUS" in
        SUCCEEDED)
            terminate 0 "Job erfolgreich abgeschlossen. Started: $STARTED_AT, Finished: $FINISHED_AT"
            ;;
        FAILED)
            terminate 2 "Der Job wurde vom Server als FAILED markiert. Started: $STARTED_AT, Finished: $FINISHED_AT"
            ;;
        DELETED)
            terminate 2 "Der Job wurde manuell oder vom System gelöscht."
            ;;
        ENQUEUED|PROCESSING|PROCESSED)
            # Job still running, continue polling
            ;;
        *)
            log_warning "Unknown status: $STATUS"
            ;;
    esac

    sleep "$POLL_INTERVAL_SECONDS"
done

terminate 1 "Timeout: Der Job ist nach $MAX_POLL_ATTEMPTS Versuchen noch nicht fertig."
