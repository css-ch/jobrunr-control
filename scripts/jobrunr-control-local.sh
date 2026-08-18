#!/usr/bin/env bash

# Concise HTTP helpers for local development and AI-agent test runs.
# For authenticated/OIDC environments use start-and-poll-job.sh instead.

set -Eeuo pipefail

readonly LOCAL_ORIGIN="${JOBRUNR_CONTROL_LOCAL_ORIGIN:-http://localhost:9090}"
readonly CONTROL_URL="${JOBRUNR_CONTROL_LOCAL_UI_URL:-${LOCAL_ORIGIN%/}/q/jobrunr-control}"
readonly CONTROL_API_URL="${JOBRUNR_CONTROL_LOCAL_API_URL:-${LOCAL_ORIGIN%/}/api/q/jobrunr-control/api}"
readonly JOBRUNR_URL="${JOBRUNR_LOCAL_API_URL:-${LOCAL_ORIGIN%/}/q/jobrunr}"
readonly POLL_INTERVAL_SECONDS="${JOBRUNR_CONTROL_LOCAL_POLL_INTERVAL:-1}"

readonly UUID_PATTERN='[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}'

usage() {
    cat <<'EOF'
Usage: scripts/jobrunr-control-local.sh <command> [arguments]

Local UI workflow:
  health
  create-template JOB_TYPE JOB_NAME [PARAMETER=VALUE ...]
  start-template TEMPLATE_ID [POSTFIX]
  history [SEARCH]
  details JOB_ID JOB_TYPE JOB_NAME
  recap JOB_ID
  messages JOB_ID JOB_TYPE [LEVEL] [TEXT]

Machine-readable inspection:
  status JOB_ID
  raw-job JOB_ID [JOB_ID ...]
  wait JOB_ID [TIMEOUT_SECONDS]

Parameter names passed to create-template may be written either as chunkSize=2
or parameters.chunkSize=2. The command prints only the created template UUID.
start-template uses the REST endpoint and prints only the new execution UUID.

Environment:
  JOBRUNR_CONTROL_LOCAL_ORIGIN         default: http://localhost:9090
  JOBRUNR_CONTROL_LOCAL_UI_URL         default: ORIGIN/q/jobrunr-control
  JOBRUNR_CONTROL_LOCAL_API_URL        default: ORIGIN/api/q/jobrunr-control/api
  JOBRUNR_LOCAL_API_URL                default: ORIGIN/q/jobrunr
  JOBRUNR_CONTROL_LOCAL_POLL_INTERVAL  default: 1 second
EOF
}

fail() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "Required command is missing: $1"
}

require_uuid() {
    [[ "$1" =~ ^${UUID_PATTERN}$ ]] || fail "Expected UUID, got: $1"
}

curl_local() {
    curl --fail-with-body --silent --show-error "$@"
}

create_template() {
    [[ $# -ge 2 ]] || fail "create-template requires JOB_TYPE and JOB_NAME"
    local job_type="$1"
    local job_name="$2"
    shift 2

    local -a form_data=(
        --data-urlencode "jobType=${job_type}"
        --data-urlencode "jobName=${job_name}"
    )
    local parameter key
    for parameter in "$@"; do
        [[ "$parameter" == *=* ]] || fail "Expected PARAMETER=VALUE, got: $parameter"
        key="${parameter%%=*}"
        if [[ "$key" != parameters.* ]]; then
            parameter="parameters.${parameter}"
        fi
        form_data+=(--data-urlencode "$parameter")
    done

    local response
    response="$(curl_local "${CONTROL_URL}/templates" \
        --request POST \
        --header 'HX-Request: true' \
        --header 'HX-Target: jobs-table' \
        "${form_data[@]}")"

    # The UI deliberately returns HTTP 200 for form validation errors and retargets the
    # response to its alert container. Make that contract fail fast for shell callers.
    if grep -q 'alert-danger' <<<"$response"; then
        local error_text
        error_text="$(printf '%s' "$response" |
            sed -E 's/<[^>]+>/ /g; s/&quot;/"/g; s/&#39;/'"'"'/g; s/&amp;/\&/g; s/[[:space:]]+/ /g')"
        fail "Template creation rejected:${error_text}"
    fi

    local -a ids=()
    mapfile -t ids < <(printf '%s' "$response" | grep -Eo "$UUID_PATTERN" | sort -u)
    if [[ ${#ids[@]} -eq 0 ]]; then
        local table
        table="$(curl_local --get "${CONTROL_URL}/templates/table" \
            --data-urlencode "search=${job_name}" \
            --data-urlencode 'size=100')"
        mapfile -t ids < <(printf '%s' "$table" | grep -Eo "$UUID_PATTERN" | sort -u)
    fi
    if [[ ${#ids[@]} -ne 1 ]]; then
        fail "Created template, but found ${#ids[@]} matching UUIDs for job name '${job_name}'"
    fi
    printf '%s\n' "${ids[0]}"
}

start_template() {
    [[ $# -ge 1 && $# -le 2 ]] || fail "start-template requires TEMPLATE_ID and optional POSTFIX"
    local template_id="$1"
    local postfix="${2:-}"
    require_uuid "$template_id"
    require_command jq

    local payload='{}'
    if [[ -n "$postfix" ]]; then
        payload="$(jq --null-input --compact-output --arg postfix "$postfix" '{postfix: $postfix}')"
    fi

    curl_local "${CONTROL_API_URL}/jobs/${template_id}/start" \
        --request POST \
        --header 'Content-Type: application/json' \
        --data "$payload" |
        jq --exit-status --raw-output '.jobId'
}

history() {
    local search="${1:-}"
    curl_local --get "${CONTROL_URL}/history/table" \
        --data-urlencode "search=${search}" \
        --data-urlencode 'size=100' \
        --data-urlencode 'sortBy=startedAt' \
        --data-urlencode 'sortOrder=desc'
}

details() {
    [[ $# -eq 3 ]] || fail "details requires JOB_ID JOB_TYPE JOB_NAME"
    require_uuid "$1"
    curl_local --get "${CONTROL_URL}/history/details" \
        --data-urlencode "jobId=$1" \
        --data-urlencode "jobType=$2" \
        --data-urlencode "jobName=$3"
}

recap() {
    [[ $# -eq 1 ]] || fail "recap requires JOB_ID"
    require_uuid "$1"
    curl_local --get "${CONTROL_URL}/history/details/recap" \
        --data-urlencode "jobId=$1"
}

messages() {
    [[ $# -ge 2 && $# -le 4 ]] || fail "messages requires JOB_ID JOB_TYPE and optional LEVEL TEXT"
    require_uuid "$1"
    curl_local --get "${CONTROL_URL}/history/details/messages" \
        --data-urlencode "jobId=$1" \
        --data-urlencode "jobType=$2" \
        --data-urlencode "search=${3:-ALL}" \
        --data-urlencode "textSearch=${4:-}" \
        --data-urlencode 'sortOrder=OLDEST_FIRST' \
        --data-urlencode 'size=100'
}

status() {
    [[ $# -eq 1 ]] || fail "status requires JOB_ID"
    require_uuid "$1"
    require_command jq
    curl_local "${CONTROL_API_URL}/jobs/$1" | jq .
}

raw_job() {
    [[ $# -ge 1 ]] || fail "raw-job requires at least one JOB_ID"
    require_command jq
    local job_id
    for job_id in "$@"; do
        require_uuid "$job_id"
        curl_local "${JOBRUNR_URL}/api/jobs/${job_id}" | jq .
    done
}

wait_for_job() {
    [[ $# -ge 1 && $# -le 2 ]] || fail "wait requires JOB_ID and optional TIMEOUT_SECONDS"
    local job_id="$1"
    local timeout_seconds="${2:-120}"
    require_uuid "$job_id"
    [[ "$timeout_seconds" =~ ^[0-9]+$ ]] || fail "TIMEOUT_SECONDS must be a non-negative integer"
    require_command jq

    local deadline=$((SECONDS + timeout_seconds))
    local response job_status summary last_summary=''
    while (( SECONDS <= deadline )); do
        response="$(curl_local "${CONTROL_API_URL}/jobs/${job_id}")"
        job_status="$(jq --exit-status --raw-output '.status' <<<"$response")"
        summary="$(jq --compact-output '{jobId, status, batchProgress, result, resultCode}' <<<"$response")"
        if [[ "$summary" != "$last_summary" ]]; then
            printf '%s\n' "$summary"
            last_summary="$summary"
        fi
        case "$job_status" in
            SUCCEEDED)
                return 0
                ;;
            FAILED|DELETED)
                return 2
                ;;
        esac
        sleep "$POLL_INTERVAL_SECONDS"
    done
    printf 'Timed out after %s seconds while waiting for job %s\n' "$timeout_seconds" "$job_id" >&2
    return 3
}

main() {
    require_command curl
    local command="${1:-help}"
    if [[ $# -gt 0 ]]; then
        shift
    fi

    case "$command" in
        health)
            curl_local "${CONTROL_URL}/history" >/dev/null
            printf 'ready: %s\n' "$CONTROL_URL"
            ;;
        create-template)
            create_template "$@"
            ;;
        start-template)
            start_template "$@"
            ;;
        history)
            history "$@"
            ;;
        details)
            details "$@"
            ;;
        recap)
            recap "$@"
            ;;
        messages)
            messages "$@"
            ;;
        status)
            status "$@"
            ;;
        raw-job)
            raw_job "$@"
            ;;
        wait)
            wait_for_job "$@"
            ;;
        help|-h|--help)
            usage
            ;;
        *)
            usage >&2
            fail "Unknown command: $command"
            ;;
    esac
}

main "$@"
