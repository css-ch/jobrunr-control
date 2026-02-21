# JobRunr Control

Quarkus extension providing a web-based control dashboard and REST API for managing and monitoring jobs in JobRunr Pro.

## Features

- **Web UI** - Bootstrap 5 + htmx-based interface for managing scheduled jobs
- **REST API** - External trigger endpoints for job execution
- **Job Discovery** - Automatic discovery of `@ConfigurableJob` implementations at build time
- **Job Parameters** - Type-safe parameter handling with validation
- **Execution History** - Monitor job executions and batch progress
- **Security** - Role-based access control with separate UI and REST API roles
- **Batch Job Support** - Create and monitor batch jobs with real-time progress tracking
- **Job Chain Status** - Accurate status evaluation for jobs with continuation chains

## Requirements

- Java 21+
- Quarkus 3.31.2
- JobRunr Pro 8.4.2 (license required)

## Installation

Add the extension to your Quarkus project:

```xml

<dependency>
    <groupId>ch.css.quarkus</groupId>
    <artifactId>quarkus-jobrunr-control</artifactId>
    <version>1.3.1</version>
</dependency>
```

The extension automatically brings in:

- JobRunr Pro 8.4.2
- Qute templates
- htmx 2.0.8
- Bootstrap 5.3.8
- Bootstrap Icons

## Configuration

### Extension Configuration

```properties
# Enable/disable UI (default: true)
quarkus.jobrunr-control.ui.enabled=true
# Enable/disable REST API (default: true)
quarkus.jobrunr-control.api.enabled=true
```

### Base Path Configuration

Use standard Quarkus HTTP configuration to customize paths:

```properties
# Set custom root path for all endpoints
quarkus.http.root-path=/scheduler
# Or set REST-specific path
quarkus.rest.path=/api
```

### JobRunr Configuration

```properties
# Enable JobRunr components
quarkus.jobrunr.background-job-server.enabled=true
quarkus.jobrunr.dashboard.enabled=true
quarkus.jobrunr.job-scheduler.enabled=true
# Database
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/yourdb
```

## Usage

### 1. Create the JobRequest

Define your parameters as a Java record:

```java
public record MyJobRequest(
        @JobParameterDefinition(name = "Message", defaultValue = "Hello")
        String message,
        @JobParameterDefinition(defaultValue = "1")
        Integer count
) implements JobRequest {

    @Override
    public Class<MyJobHandler> getJobRequestHandler() {
        return MyJobHandler.class;
    }
}
```

### 2. Implement the JobRequestHandler

```java

@ApplicationScoped
public class MyJobHandler implements JobRequestHandler<MyJobRequest> {

    @ConfigurableJob(name = "My Custom Job")
    @Override
    public void run(MyJobRequest request) throws Exception {
        jobContext().logger().info("Processing: " + request.message());
    }
}
```

### 3. Access the UI

Navigate to `http://localhost:9090/q/jobrunr-control` to:

- View all discovered jobs
- Schedule jobs with parameters
- Monitor execution history
- Track batch job progress

### 4. Use REST API

Start a job or template externally:

```bash
# Start a job (or template) immediately
curl -X POST http://localhost:9090/q/jobrunr-control/api/jobs/{jobId}/start \
  -H "Content-Type: application/json" \
  -d '{
    "postfix": "20240127",
    "parameters": {
      "message": "Hello",
      "count": 5
    }
  }'

# Check job status
curl http://localhost:9090/q/jobrunr-control/api/jobs/{jobId}
```

## UI Endpoints

Base path: `/q/jobrunr-control`

| Path          | Description             |
|---------------|-------------------------|
| `/`           | Redirects to jobs list  |
| `/jobs`       | List all scheduled jobs |
| `/templates`  | Manage template jobs    |
| `/executions` | View execution history  |

## REST API Endpoints

Base path: `/q/jobrunr-control/api`

| Method | Path                  | Roles Required                        | Description                                                                   |
|--------|-----------------------|---------------------------------------|-------------------------------------------------------------------------------|
| POST   | `/jobs/{jobId}/start` | `api-executor`, `admin`               | Start a job immediately. If the job is a template, it is cloned and executed. |
| GET    | `/jobs/{jobId}`       | `api-reader`, `api-executor`, `admin` | Get job status and progress                                                   |

**Note:** All REST API endpoints require authentication and appropriate role assignment.

## Security

The extension provides role-based access control (RBAC) with **two distinct role sets** for UI and REST API:

### Web UI Roles

Access to the Web UI at `/q/jobrunr-control` is controlled by:

- **viewer**: Read-only access (view scheduled jobs and execution history)
- **configurator**: All viewer permissions plus create, edit, and delete jobs
- **admin**: All configurator permissions plus immediate job execution

### REST API Roles

Access to the REST API at `/q/jobrunr-control/api` uses separate roles:

- **api-reader**: Read-only access to GET endpoints (job status checks)
- **api-executor**: All api-reader permissions plus POST endpoints (start jobs)
- **admin**: Full access to all operations (shared with UI)

> **Why separate role sets?**
>
> This design allows organizations to:
> - Grant operators UI access without exposing REST API credentials
> - Issue API keys to external systems (CI/CD pipelines) with limited permissions
> - Use different authentication methods for UI vs. API (form login vs. bearer tokens)

### Configuration Examples

**Development Mode** (all roles granted automatically):

```properties
%dev.quarkus.security.users.embedded.enabled=true
%dev.quarkus.security.users.embedded.plain-text=true
%dev.quarkus.security.users.embedded.users.admin=admin
%dev.quarkus.security.users.embedded.roles.admin=admin,viewer,configurator,api-reader,api-executor
```

**Production with OIDC/OAuth2**:

```properties
quarkus.oidc.auth-server-url=https://your-keycloak.com/realms/your-realm
quarkus.oidc.client-id=jobrunr-control
quarkus.oidc.credentials.secret=${OIDC_CLIENT_SECRET}
quarkus.oidc.roles.source=accesstoken
```

Configure your OIDC provider to issue the appropriate roles:

- For UI users: `viewer`, `configurator`, `admin`
- For API clients: `api-reader`, `api-executor`, `admin`

**Production with Basic Auth**:

```properties
quarkus.security.users.embedded.enabled=true
quarkus.security.users.embedded.users.operator=${OPERATOR_PASSWORD}
quarkus.security.users.embedded.roles.operator=viewer,configurator
quarkus.security.users.embedded.users.api-client=${API_CLIENT_PASSWORD}
quarkus.security.users.embedded.roles.api-client=api-reader,api-executor
```

For detailed security setup, see the [Programmer's Guide](docs/programmers.adoc#_security_configuration).

### Development without OIDC

For local development and testing, you can disable OIDC authentication:

```properties
quarkus.oidc.enabled=false
```

**⚠️ WARNING**: This grants all roles (admin, configurator, viewer, api-reader, api-executor)
to ALL requests without authentication. Use ONLY in development and testing environments!

When OIDC is disabled:

- No login required
- No logout button in UI
- All requests are treated as "anonymous-user" with full permissions
- Audit logs show "anonymous-user" as the actor

**Production deployments MUST use OIDC authentication** (enabled by default).

### Dev Mode

For development and testing, grant all roles automatically:

```properties
dev.test.roles=admin,viewer,configurator,api-reader,api-executor
```

## Key Features

### Supported Parameter Types

| Type       | Format                    | Example                      |
|------------|---------------------------|------------------------------|
| String     | Plain text                | `"Hello World"`              |
| Multiline  | Multi-line text           | `"Line 1\nLine 2\nLine 3"`   |
| Integer    | Whole number              | `42`                         |
| Double     | Decimal number            | `3.14159`                    |
| Boolean    | true/false                | `true`                       |
| Date       | ISO date                  | `2024-01-15`                 |
| DateTime   | ISO datetime              | `2024-01-15T10:30:00`        |
| Enum       | Enum constant name        | `OPTION_A`                   |
| Multi-Enum | Comma-separated enum list | `OPTION_A,OPTION_B,OPTION_C` |

### Parameter Storage Strategies

- **Inline Storage** (default): Parameters stored directly in JobRunr's job table
- **External Storage**: Parameters stored in a separate database table for large parameter sets

Use `@JobParameterSet` on a JobRequest to enable external storage. Requires Hibernate ORM.

### Job Chain Status Evaluation

For jobs with continuation chains (`continueWith()` or `onFailure()`), the extension evaluates the overall chain status:

- A chain is **complete** when all relevant leaf jobs have finished (SUCCEEDED, FAILED, or DELETED)
- A chain is **in progress** when any leaf job is still running (ENQUEUED, PROCESSING, PROCESSED)
- A chain **succeeded** when all executed leaf jobs succeeded
- A chain **failed** when any executed leaf job failed

## Advanced Configuration

### Parameter Storage

```properties
# Storage strategy: INLINE (default) or EXTERNAL
quarkus.jobrunr-control.parameter-storage.strategy=INLINE
# Persistence unit for external parameter storage (build-time config)
# Default: <default> (Hibernate ORM default persistence unit)
quarkus.jobrunr-control.parameter-storage.persistence-unit-name=<default>
# Cleanup configuration for external parameter storage
quarkus.jobrunr-control.parameter-storage.cleanup.enabled=true
quarkus.jobrunr-control.parameter-storage.cleanup.retention-days=30
```

### Batch Progress Timeout

```properties
jobrunr.batch-progress.timeout=PT5S
```

### Cache Configuration

```properties
quarkus.cache.caffeine.job-definitions.expire-after-write=15M
```

### JobRunr Dashboard Link

```properties
jobrunr.dashboard.url=http://localhost:8000
```

## Development Database Setup

The example application uses **H2 in-memory** by default for development, requiring no external setup.
For Oracle or PostgreSQL, use the provided shell scripts to start and configure the database.

### Default: H2 (no setup required)

```bash
./mvnw -f jobrunr-control-example/pom.xml quarkus:dev
```

H2 runs in-memory in **standard mode** and automatically creates the `jobrunr_control_parameter_sets` table on startup.
Data is reset on every restart.

**Important:** H2 cannot run in Oracle compatibility mode because JobRunr generates H2-specific SQL syntax (e.g.,
`LIMIT` clause).
For Oracle compatibility testing, use an actual Oracle database with `./start-oracle.sh` and the `oracle` profile.

**Note:** H2 uses `CLOB` instead of `JSON` type for the `parameters_json` column to avoid double-serialization issues
with JDBC drivers.

### PostgreSQL

**Prerequisites:** Docker or Podman installed.

1. Start PostgreSQL and create the required table:

   ```bash
   ./start-postgres.sh
   ```

   The script always creates a fresh `postgres:15` container on port 5432 and creates the
   `jobrunr_control_parameter_sets` table automatically. Any existing container is removed first.

2. Start the application with the `postgres` profile:

   ```bash
   ./mvnw -f jobrunr-control-example/pom.xml quarkus:dev -Dquarkus.profile=dev,postgres
   ```

**Connection details** (configured in `application.properties` under `%postgres`):

| Property | Value                                       |
|----------|---------------------------------------------|
| JDBC URL | `jdbc:postgresql://localhost:5432/postgres` |
| Username | `postgres`                                  |
| Password | `your_strong_password`                      |

### Oracle

**Prerequisites:** Docker or Podman installed. You must be logged in to the Oracle Container Registry:

```bash
docker login container-registry.oracle.com
```

1. Start Oracle and create the required table:

   ```bash
   ./start-oracle.sh
   ```

   The script always creates a fresh Oracle Database Free container on port 1521, creates a `JOBRUNR_DATA`
   tablespace, and creates the `jobrunr_control_parameter_sets` table automatically.
   Any existing container is removed first. First startup may take 2–5 minutes.

2. Start the application with the `oracle` profile:

   ```bash
   ./mvnw -f jobrunr-control-example/pom.xml quarkus:dev -Dquarkus.profile=dev,oracle
   ```

**Connection details** (configured in `application.properties` under `%oracle`):

| Property | Value                                       |
|----------|---------------------------------------------|
| JDBC URL | `jdbc:oracle:thin:@localhost:1521/FREEPDB1` |
| Username | `system`                                    |
| Password | `YourStrongPassword123`                     |

### Manual Table Creation

If you manage the database yourself, create the `jobrunr_control_parameter_sets` table using the
SQL scripts in `docs/sql/`:

| Database      | Script                    |
|---------------|---------------------------|
| PostgreSQL    | `docs/sql/postgresql.sql` |
| Oracle        | `docs/sql/oracle.sql`     |
| H2            | `docs/sql/h2.sql`         |
| MySQL/MariaDB | `docs/sql/mysql.sql`      |

This table is only required if any job uses `@JobParameterSet` for external parameter storage.

## Architecture

The extension follows Clean Architecture principles:

```text
runtime/
├── domain/           # Core models and ports
├── application/      # Use cases
├── infrastructure/   # JobRunr integration
├── adapter/
│   ├── rest/        # REST API
│   └── ui/          # UI controllers
└── dev/             # Dev mode features
```

See [Architecture Documentation](docs/arc42.adoc) for full details.

## Documentation

| Document                                      | Description                                     |
|-----------------------------------------------|-------------------------------------------------|
| [Architecture Documentation](docs/arc42.adoc) | Technical architecture following arc42 template |
| [User Guide](docs/user.adoc)                  | End-user guide for operating the dashboard      |
| [Programmer's Guide](docs/programmers.adoc)   | Developer guide for implementing jobs           |

## Development

### Build

```bash
./mvnw clean compile
```

### Full Build with Tests

```bash
./mvnw clean verify
```

### Run in Dev Mode

```bash
cd jobrunr-control-example
../mvnw quarkus:dev
```

### Authentication in Dev Mode

By default, dev mode starts with **Keycloak DevServices** using Testcontainers:

- **Automatic Setup**: Keycloak container starts automatically with a pre-configured realm
- **Pre-configured Users**:
    - `admin` / `admin` - Full access (all roles)
    - `configurator` / `configurator` - Can create/modify templates
    - `viewer` / `viewer` - Read-only access

See [DEV-MODE-KEYCLOAK.md](jobrunr-control-example/DEV-MODE-KEYCLOAK.md) for detailed setup and customization.

### Troubleshooting Dev Mode Startup

#### Application Hangs on Startup with Podman

**Symptom:** Application hangs during startup in dev mode, port 9090 doesn't open, no error displayed. Tests work fine.

**Root Cause:** The OIDC Dev UI attempts to discover metadata from Keycloak before the DevServices container is fully
ready. With Podman, this causes an indefinite hang.

**Solutions:**

1. **Use the no-docker profile** (bypasses OIDC completely):
   ```bash
   ./mvnw -f jobrunr-control-example/pom.xml quarkus:dev -Dquarkus.profile=dev,no-docker
   ```

2. **Disable OIDC Dev UI** in `application.properties` (already configured):
   ```properties
   %dev.quarkus.oidc.dev-ui.enabled=false
   ```

3. **Use external Keycloak** (start separately):
   ```bash
   ./start-keycloak.sh
   ./mvnw -f jobrunr-control-example/pom.xml quarkus:dev -Dquarkus.profile=dev,start-keycloak
   ```

**Diagnostic Commands:**

```bash
# Check if process is hanging
ps aux | grep quarkus

# Get thread dump (replace PID)
jstack <PID> | grep -A 10 "Quarkus Main Thread"

# If you see "OidcDevUiRecorder.discoverMetadata", it's the OIDC Dev UI issue
```

**Note:** This is a known limitation with Quarkus OIDC DevServices when using Podman. The fix (
`%dev.quarkus.oidc.dev-ui.enabled=false`) is already applied in the configuration.

### Access Points

- JobRunr Control: `http://localhost:9090/q/jobrunr-control`
- JobRunr Dashboard: `http://localhost:9090/q/jobrunr`
- Swagger UI: `http://localhost:9090/q/swagger-ui/`
- Keycloak Admin: Check console output for dynamic URL

## License

Internal CSS Project. Requires a valid JobRunr Pro license.

## Support

For issues and questions, contact the JobRunr Control Team.

