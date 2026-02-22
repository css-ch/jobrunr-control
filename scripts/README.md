# JobRunr Control - API Scripts

This directory contains utility scripts for interacting with the JobRunr Control REST API.

## Quick Start (Local Development)

```bash
# 1. Start Keycloak
./start-keycloak.sh

# 2. Start JobRunr Control with external Keycloak
./mvnw -f jobrunr-control-example/pom.xml quarkus:dev -Dquarkus.profile=dev,keycloak

# 3. Set OIDC configuration (required)
export OIDC_CLIENT_ID="jobrunr-control"
export OIDC_CLIENT_SECRET="jobrunr-secret"
export OIDC_SERVER="http://localhost:8080"
export OIDC_REALM="jobrunr"

# 4. Run batch script with OIDC authentication
./scripts/start-and-poll-job.sh <template-id> "batch-$(date +%Y%m%d)"
```

**Alternative:** Use the example script with pre-configured OIDC defaults:

```bash
export BATCH_TEMPLATE_ID="<your-template-id>"
./scripts/example-oidc-batch.sh
```

For detailed OIDC setup instructions, see [OIDC-SETUP.md](OIDC-SETUP.md).

## Start and Poll Job Script

Starts a job template via the REST API and polls until completion (or failure).

### Features

- ✅ Start job templates with custom postfix and parameters
- ✅ Automatic polling until job completes
- ✅ Real-time batch progress tracking
- ✅ Type detection for parameters (string, integer, float, boolean)
- ✅ Configurable timeouts and poll intervals
- ✅ Colored output for better readability
- ✅ OIDC authentication with automatic token refresh

### Usage

```bash
./start-and-poll-job.sh <job-id> <postfix> [key1=value1] [key2=value2] ...
```

### Parameters

| Parameter | Type      | Required | Description                              |
|-----------|-----------|----------|------------------------------------------|
| `job-id`  | UUID      | Yes      | UUID of the template job to start        |
| `postfix` | String    | Yes      | Postfix to append to the job name        |
| `key=val` | Key-Value | No       | Additional parameters to pass to the job |

### Environment Variables

| Variable                    | Default                 | Description                                    |
|-----------------------------|-------------------------|------------------------------------------------|
| `JOBRUNR_CONTROL_URL`       | `http://localhost:9090` | Base URL of JobRunr Control                    |
| **Basic Auth (Legacy)**     |                         |                                                |
| `JOBRUNR_USER`              | `admin`                 | Username for authentication                    |
| `JOBRUNR_PASSWORD`          | `admin`                 | Password for authentication                    |
| **OIDC Auth (Recommended)** |                         |                                                |
| `OIDC_CLIENT_ID`            | -                       | OIDC client ID (required for OIDC)             |
| `OIDC_CLIENT_SECRET`        | -                       | OIDC client secret (required for OIDC)         |
| `OIDC_TOKEN_URL`            | Auto-detected           | Token endpoint URL (optional)                  |
| `OIDC_SERVER`               | -                       | Keycloak server URL (required for auto-detect) |
| `OIDC_REALM`                | -                       | Keycloak realm name (required for auto-detect) |
| **Polling**                 |                         |                                                |
| `POLL_INTERVAL`             | `5`                     | Poll interval in seconds                       |
| `MAX_ATTEMPTS`              | `720`                   | Maximum poll attempts (720 × 5s = 1 hour)      |

**Note:** The main script `start-and-poll-job.sh` requires explicit OIDC configuration. See `example-oidc-batch.sh` for
local development defaults (`jobrunr-control` / `jobrunr-secret`).

### Authentication

The script supports two authentication methods:

#### 1. OIDC Client Credentials Flow (Recommended)

For production and local development with external Keycloak:

**Local Development Setup:**

```bash
# 1. Start external Keycloak with pre-configured realm
./start-keycloak.sh

# 2. Start JobRunr Control with external Keycloak profile
./mvnw -f jobrunr-control-example/pom.xml quarkus:dev -Dquarkus.profile=dev,keycloak

# 3. Use the script with OIDC authentication
export OIDC_CLIENT_ID="jobrunr-control"
export OIDC_CLIENT_SECRET="jobrunr-secret"
export OIDC_SERVER="http://localhost:8080"
export OIDC_REALM="jobrunr"

./scripts/start-and-poll-job.sh "550e8400-e29b-41d4-a716-446655440000" "batch-$(date +%Y%m%d)"
```

**Production Setup:**

```bash
export OIDC_CLIENT_ID="jobrunr-control"
export OIDC_CLIENT_SECRET="${VAULT_SECRET}"  # From secret manager
export OIDC_SERVER="https://keycloak.prod.example.com"
export OIDC_REALM="jobrunr"

./scripts/start-and-poll-job.sh "${TEMPLATE_ID}" "prod-$(date +%Y%m%d)"
```

**Token Refresh**: The script automatically refreshes the access token 30 seconds before expiry, making it suitable for
long-running jobs (hours/days).

**Auto-Detection**: If `OIDC_TOKEN_URL` is not provided, it will be auto-detected as:

```
{OIDC_SERVER}/realms/{OIDC_REALM}/protocol/openid-connect/token
```

#### 2. Basic Authentication (Development/Testing Only)

For quick local testing without Keycloak (requires `quarkus.oidc.enabled=false`):

```bash
# Only works when OIDC is disabled in application.properties
export JOBRUNR_USER="admin"
export JOBRUNR_PASSWORD="admin"

./start-and-poll-job.sh "550e8400-e29b-41d4-a716-446655440000" "batch-20240222"
```

### Examples

#### Local Development with External Keycloak (Recommended)

```bash
# 1. Start Keycloak
./start-keycloak.sh

# 2. Start JobRunr Control with external Keycloak
./mvnw -f jobrunr-control-example/pom.xml quarkus:dev -Dquarkus.profile=dev,keycloak

# 3. Set all required OIDC variables
export OIDC_CLIENT_ID="jobrunr-control"
export OIDC_CLIENT_SECRET="jobrunr-secret"
export OIDC_SERVER="http://localhost:8080"
export OIDC_REALM="jobrunr"

# 4. Run script
./scripts/start-and-poll-job.sh \
  "550e8400-e29b-41d4-a716-446655440000" \
  "batch-$(date +%Y%m%d)"
```

**Using the example script** (OIDC defaults pre-configured):

```bash
# Set template ID
export BATCH_TEMPLATE_ID="550e8400-e29b-41d4-a716-446655440000"

# Run example script - OIDC defaults already configured
./scripts/example-oidc-batch.sh
```

#### Job with Parameters

```bash
export OIDC_CLIENT_ID="jobrunr-control"
export OIDC_CLIENT_SECRET="jobrunr-secret"
export OIDC_SERVER="http://localhost:8080"
export OIDC_REALM="jobrunr"

./scripts/start-and-poll-job.sh \
  "550e8400-e29b-41d4-a716-446655440000" \
  "daily-$(date +%Y%m%d)" \
  "message=Hello World" \
  "count=42" \
  "enabled=true"
```

#### Production with External Keycloak

```bash
export OIDC_CLIENT_ID="jobrunr-control"
export OIDC_CLIENT_SECRET="${VAULT_SECRET}"  # From secret manager
export OIDC_SERVER="https://keycloak.prod.example.com"
export OIDC_REALM="jobrunr"

./scripts/start-and-poll-job.sh \
  "550e8400-e29b-41d4-a716-446655440000" \
  "prod-batch-$(date +%Y%m%d)" \
  "environment=production"
```

#### Custom Environment

```bash
export JOBRUNR_CONTROL_URL="https://prod.example.com"
export JOBRUNR_USER="api-user"
export JOBRUNR_PASSWORD="secret123"
export POLL_INTERVAL=10
export MAX_ATTEMPTS=360

./start-and-poll-job.sh \
  "550e8400-e29b-41d4-a716-446655440000" \
  "prod-batch-$(date +%Y%m%d)"
```

### Parameter Type Detection

The script automatically detects parameter types:

| Input          | Detected Type | JSON Output       |
|----------------|---------------|-------------------|
| `count=42`     | Integer       | `"count": 42`     |
| `rate=3.14`    | Float         | `"rate": 3.14`    |
| `enabled=true` | Boolean       | `"enabled": true` |
| `name=John`    | String        | `"name": "John"`  |

### Exit Codes

| Code | Meaning                                |
|------|----------------------------------------|
| 0    | Job completed successfully (SUCCEEDED) |
| 1    | Job failed (FAILED) or API error       |
| 2    | Job was deleted (DELETED)              |
| 3    | Timeout reached (job still running)    |

### Output Example

```
[INFO] Starting job template: 550e8400-e29b-41d4-a716-446655440000
[INFO] Postfix: batch-20240127
[INFO] Parameters: {"message":"Hello World","count":42}

[INFO] POST http://localhost:9090/q/jobrunr-control/api/jobs/550e8400-e29b-41d4-a716-446655440000/start
[SUCCESS] Job started successfully!
[INFO] Job ID: 660e8400-e29b-41d4-a716-446655440001
[INFO] Message: Job started successfully

[INFO] Polling for job completion (interval: 5s, max attempts: 720)...

[INFO] [1] Status: ENQUEUED
[INFO] [2] Status: PROCESSING
[INFO] [3] Status: PROCESSING | Progress: 5/100 succeeded, 0 failed, 95 pending (5.0%)
[INFO] [4] Status: PROCESSING | Progress: 23/100 succeeded, 0 failed, 77 pending (23.0%)
[INFO] [5] Status: PROCESSING | Progress: 47/100 succeeded, 0 failed, 53 pending (47.0%)
[INFO] [6] Status: PROCESSING | Progress: 78/100 succeeded, 0 failed, 22 pending (78.0%)
[INFO] [7] Status: SUCCEEDED | Progress: 100/100 succeeded, 0 failed, 0 pending (100.0%)
[SUCCESS] Job completed successfully!
[INFO] Started at: 2024-01-27T10:30:00Z
[INFO] Finished at: 2024-01-27T10:32:15Z
[INFO] Final Progress: 100/100 succeeded, 0 failed
```

### Prerequisites

- `bash` (pre-installed on macOS and most Linux distributions)
- `curl` (pre-installed on macOS and most Linux distributions)

### Integration Examples

#### GitHub Actions (with OIDC)

```yaml
name: Run Batch Job

on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM
  workflow_dispatch:

jobs:
  run-batch:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v3

      - name: Start and Poll Job (OIDC)
        env:
          JOBRUNR_CONTROL_URL: ${{ secrets.JOBRUNR_URL }}
          OIDC_CLIENT_ID: ${{ secrets.OIDC_CLIENT_ID }}
          OIDC_CLIENT_SECRET: ${{ secrets.OIDC_CLIENT_SECRET }}
          OIDC_SERVER: ${{ secrets.OIDC_SERVER }}
          OIDC_REALM: jobrunr
        run: |
          ./scripts/start-and-poll-job.sh \
            "${{ secrets.JOB_TEMPLATE_ID }}" \
            "daily-$(date +%Y%m%d)" \
            "environment=production"
```

#### GitLab CI (with OIDC)

```yaml
batch-job:
  stage: deploy
  script:
    - export JOBRUNR_CONTROL_URL="$JOBRUNR_URL"
    - export OIDC_CLIENT_ID="$OIDC_CLIENT_ID"
    - export OIDC_CLIENT_SECRET="$OIDC_CLIENT_SECRET"
    - export OIDC_SERVER="$OIDC_SERVER"
    - |
      ./scripts/start-and-poll-job.sh \
        "$JOB_TEMPLATE_ID" \
        "daily-$(date +%Y%m%d)"
  only:
    - schedules
```

#### Cron Job (Unix)

```bash
# /etc/cron.d/jobrunr-daily-batch
0 2 * * * jobrunr-user /opt/jobrunr/scripts/start-and-poll-job.sh \
  "550e8400-e29b-41d4-a716-446655440000" \
  "daily-$(date +\%Y\%m\%d)" \
  >> /var/log/jobrunr-batch.log 2>&1
```

**Environment setup for cron** (`/etc/environment` or in script):

```bash
JOBRUNR_CONTROL_URL=https://jobrunr.example.com
OIDC_CLIENT_ID=jobrunr-control
OIDC_CLIENT_SECRET=your-secret-here
OIDC_SERVER=https://keycloak.example.com
OIDC_REALM=jobrunr
```

### Troubleshooting

#### Authentication Errors (401/403)

**Basic Auth:**

```bash
# Check credentials
export JOBRUNR_USER="your-username"
export JOBRUNR_PASSWORD="your-password"
```

**OIDC:**

```bash
# Verify client credentials are correct
export OIDC_CLIENT_ID="jobrunr-control"
export OIDC_CLIENT_SECRET="your-secret"

# Test token acquisition manually
curl -X POST "http://localhost:8080/realms/jobrunr/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=jobrunr-control" \
  -d "client_secret=your-secret"

# Check if client has correct roles/permissions in Keycloak
# Required roles: api-reader, api-executor (or admin)
```

#### Token Refresh Issues

The script automatically refreshes tokens 30 seconds before expiry. If you see repeated token refresh failures:

```bash
# Increase the refresh buffer
TOKEN_REFRESH_BUFFER=60  # Refresh 60s before expiry

# Check token expiry time in Keycloak
# Admin Console -> Realm Settings -> Tokens -> Access Token Lifespan
```

#### Connection Errors

```bash
# Test connectivity
curl -v http://localhost:9090/q/jobrunr-control/api/jobs/<job-id>

# Check if JobRunr Control is running
curl http://localhost:9090/q/health

# Test OIDC server connectivity
curl http://localhost:8080/realms/jobrunr/.well-known/openid-configuration
```

#### Job Not Found (404)

```bash
# Verify the job ID exists
curl -u admin:admin http://localhost:9090/q/jobrunr-control/api/jobs/<job-id>

# Check if it's a template job (only templates can be started via API)
```

#### Timeout Issues

```bash
# Increase timeout for long-running jobs
export POLL_INTERVAL=10
export MAX_ATTEMPTS=1800  # 5 hours (10s × 1800)
```

### API Reference

The script uses the following JobRunr Control REST API endpoints:

| Method | Endpoint                                    | Description          |
|--------|---------------------------------------------|----------------------|
| POST   | `/q/jobrunr-control/api/jobs/{jobId}/start` | Start a job template |
| GET    | `/q/jobrunr-control/api/jobs/{jobId}`       | Get job status       |

For full API documentation, see:

- OpenAPI spec: `http://localhost:9090/q/openapi`
- Swagger UI: `http://localhost:9090/q/swagger-ui/`

### Security Notes

#### Authentication

- **Prefer OIDC over Basic Auth**: Use OIDC client credentials flow in production for better security.
- **Do not hardcode credentials** in scripts. Use environment variables or secret managers (e.g., HashiCorp Vault, AWS
  Secrets Manager).
- **Use HTTPS** in production environments for both JobRunr Control and OIDC server.
- **Rotate secrets** regularly for both API users and OIDC clients.

#### OIDC Configuration

- **Client Secret Protection**: Store `OIDC_CLIENT_SECRET` in a secure secret manager.
- **Token Lifespan**: Configure appropriate token lifespans in Keycloak (e.g., 5-15 minutes for access tokens).
- **Client Roles**: Grant minimal permissions to the OIDC client:
    - `api-reader` - For status checks only
    - `api-executor` - For starting jobs (recommended for automation)
    - `admin` - Only when absolutely necessary
- **Service Accounts**: Use dedicated service accounts for OIDC clients, not user accounts.

#### Network Security

- **Firewall Rules**: Restrict access to JobRunr Control and OIDC endpoints.
- **TLS/SSL**: Always use TLS 1.2+ for production environments.
- **Certificate Validation**: Ensure proper SSL certificate validation (no self-signed certs in production).

#### Audit & Monitoring

- **Audit logs**: All API calls are logged with the authenticated user/client ID.
- **Monitor token usage**: Track token refresh rates to detect anomalies.
- **Alert on failures**: Set up alerts for authentication failures or job execution errors.

#### CI/CD Best Practices

- **GitHub Actions**: Use repository secrets for credentials.
- **Jenkins**: Use credential plugins (e.g., Credentials Binding Plugin).
- **GitLab CI**: Use protected variables for sensitive data.

```yaml
# GitHub Actions Example
env:
  OIDC_CLIENT_ID: ${{ secrets.JOBRUNR_CLIENT_ID }}
  OIDC_CLIENT_SECRET: ${{ secrets.JOBRUNR_CLIENT_SECRET }}
```

### License

Internal CSS Project. Requires a valid JobRunr Pro license.

### Support

For issues and questions, contact the JobRunr Control Team.

