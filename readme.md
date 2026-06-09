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
- **Job Chain Status** - Conservative status evaluation for jobs with continuation chains

## Requirements

- Java 21+
- Quarkus 3.36.1
- JobRunr Pro 8.6.1 (license required)

## Installation

Add the extension to your Quarkus project:

```xml

<dependency>
    <groupId>ch.css.quarkus</groupId>
    <artifactId>quarkus-jobrunr-control</artifactId>
    <version>2.1.2-SNAPSHOT</version>
</dependency>
```

The extension automatically brings in:

- JobRunr Pro 8.6.1
- Qute templates
- htmx 2.0.10
- Bootstrap 5.3.8
- Bootstrap Icons

## Configuration

### Extension Configuration

```properties
# Show the UUID column in dashboard tables (default: false)
quarkus.jobrunr-control.ui.show-job-uuid=false

# Parameter storage mode: INLINE (default) or EXTERNAL
quarkus.jobrunr-control.parameter-storage.strategy=INLINE
```

### Base Path Configuration

Use standard Quarkus HTTP configuration to customize paths:

```properties
# Set custom root path for all endpoints (affects UI dashboard and REST API alike)
quarkus.http.root-path=/scheduler
# Set custom path for all JAX-RS resources (affects the consumer app's resources
# and the JobRunr Control REST API)
quarkus.rest.path=/api
```

**UI dashboard vs. REST API under custom `quarkus.rest.path` / `@ApplicationPath`:**

| Endpoint group | Path | Affected by `quarkus.rest.path` / `@ApplicationPath`? |
| --- | --- | --- |
| UI dashboard (HTML + HTMX) | `/q/jobrunr-control/*` | No — registered under the non-application root path |
| REST API (JSON, external triggers) | `/q/jobrunr-control/api/*` | Yes — prefixed, e.g. `/api/q/jobrunr-control/api/*` |

The UI is mounted as Vert.x routes under Quarkus' non-application root path
(`quarkus.http.non-application-root-path`, default `/q`), so navigation links and HTMX
targets always resolve correctly — exactly like `/q/health`, `/q/openapi` or the JobRunr
Pro dashboard.

The REST API remains a JAX-RS resource (to preserve the MicroProfile OpenAPI annotations
used in `/q/swagger-ui`). Consumers that declare `@ApplicationPath("/api")` or set
`quarkus.rest.path=/api` will therefore reach the external trigger API at
`/api/q/jobrunr-control/api/jobs/{jobRef}/start` instead of
`/q/jobrunr-control/api/jobs/{jobRef}/start`. Update HTTP auth permission paths for the
REST API accordingly, e.g.

```properties
quarkus.http.auth.permission.jobrunr-control-api-read.paths=/api/q/jobrunr-control/api/*
quarkus.http.auth.permission.jobrunr-control-api-write.paths=/api/q/jobrunr-control/api/*
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

Define your parameters as a Java record. `JobRequest` types **must be records** —
regular classes are not discovered as configurable jobs, because parameter extraction relies on
record components (canonical order, immutability, type metadata):

```java
public record MyJobRequest(
        @JobParameterDefinition(name = "Message", required = false, defaultValue = "Hello")
        String message,
        @JobParameterDefinition(required = false, defaultValue = "1")
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

> **Path note:** the extension's default REST path is `/q/jobrunr-control/api/*`.
> The bundled example application declares `@ApplicationPath("/api")`, so its effective
> path is `/api/q/jobrunr-control/api/*`.

```bash
# Start a job (or template) immediately
curl -X POST http://localhost:9090/api/q/jobrunr-control/api/jobs/{jobRef}/start \
  -H "Content-Type: application/json" \
  -d '{
    "postfix": "20240127",
    "parameters": {
      "message": "Hello",
      "count": 5
    }
  }'

# Check job status
curl http://localhost:9090/api/q/jobrunr-control/api/jobs/{jobId}
```

**Automated Script**: For batch processing and polling, use the provided script:

```bash
./scripts/start-and-poll-job.sh <job-id> <postfix> [key1=value1] [key2=value2]
```

See [scripts/README.md](scripts/README.md) for detailed documentation and examples.

## UI Endpoints

Base path: `/q/jobrunr-control`

| Path          | Description             |
|---------------|-------------------------|
| `/`           | Dashboard entry page    |
| `/scheduled`  | List all scheduled jobs |
| `/templates`  | Manage template jobs    |
| `/history`    | View execution history  |

## REST API Endpoints

Base path: `/q/jobrunr-control/api`

| Method | Path                  | Roles Required                        | Description                                                                   |
|--------|-----------------------|---------------------------------------|-------------------------------------------------------------------------------|
| POST   | `/jobs/{jobRef}/start` | `api-executor`, `admin`               | Start a job immediately. `jobRef` may be a UUID or a template name. If the target is a template, it is cloned and executed. |
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

**Development Mode** (OIDC disabled, `admin` granted automatically by `JobRunrControlRoleAugmentor`):

```properties
%dev.quarkus.oidc.enabled=false
%dev.quarkus.http.auth.permission.jobrunr-control.policy=permit
%dev.quarkus.http.auth.permission.jobrunr-dashboard.policy=permit
%dev.quarkus.http.auth.permission.jobrunr-control-api-read.policy=permit
%dev.quarkus.http.auth.permission.jobrunr-control-api-write.policy=permit
%dev.quarkus.http.auth.permission.jobrunr-api-read.policy=permit
%dev.quarkus.http.auth.permission.jobrunr-api-write.policy=permit
```

**Production with OIDC/OAuth2**:

```properties
quarkus.oidc.auth-server-url=https://your-keycloak.com/realms/your-realm
quarkus.oidc.client-id=jobrunr-control
quarkus.oidc.credentials.secret=${OIDC_CLIENT_SECRET}
quarkus.oidc.application-type=web-app
quarkus.oidc.roles.source=accesstoken
quarkus.oidc.roles.role-claim-path=realm_access/roles

# Bearer tenant for REST API service accounts only
quarkus.oidc.bearer.tenant-paths=/q/jobrunr-control/api/*
quarkus.oidc.bearer.auth-server-url=https://your-keycloak.com/realms/your-realm
quarkus.oidc.bearer.client-id=jobrunr-control
quarkus.oidc.bearer.credentials.secret=${OIDC_CLIENT_SECRET}
quarkus.oidc.bearer.application-type=service
quarkus.oidc.bearer.roles.source=accesstoken
quarkus.oidc.bearer.roles.role-claim-path=realm_access/roles
```

Configure your OIDC provider to issue the appropriate roles:

- For UI users: `viewer`, `configurator`, `admin`
- For API clients: `api-reader`, `api-executor`, `admin`

> **Important:** keep the REST tenant name `bearer`. Naming it `api` collides with Quarkus
> path-segment tenant resolution and breaks embedded JobRunr Dashboard requests.

For detailed security setup, see the [Programmer's Guide](docs/programmers.adoc).

### Development without OIDC

For local development and testing, you can disable OIDC authentication:

```properties
quarkus.oidc.enabled=false
```

**⚠️ WARNING**: This does not authenticate users. The extension augments every request with the
`admin` role so all UI and API checks pass. Use ONLY in development and testing environments.

When OIDC is disabled:

- No login required
- No logout button in UI
- All requests are treated as "anonymous-user" with full permissions
- Audit logs show "anonymous-user" as the actor

**Production deployments MUST use OIDC authentication** (enabled by default).

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

Use `@JobParameterSet(parameterSetClass = ...)` on a `JobRequest` to enable external storage.
The parameter schema lives in a separate record referenced by `parameterSetClass`, and values are
persisted in `jobrunr_control_parameter_sets` via the configured datasource.

### Job Chain Status Evaluation

For jobs with continuation chains (`continueWith()` or `onFailure()`), the extension evaluates the overall chain status conservatively:

- A chain is **complete** when all continuation leaves considered by the evaluator have finished (SUCCEEDED, FAILED, or DELETED)
- A chain is **in progress** when any considered continuation leaf is still running (ENQUEUED, PROCESSING, PROCESSED)
- A chain **succeeded** when all terminal continuation leaves considered by the evaluator succeeded
- A chain **failed** when any terminal continuation leaf considered by the evaluator failed

Because JobRunr's public API does not expose whether a continuation was created by `continueWith()`
or `onFailure()`, the implementation may temporarily treat both continuation types as potentially relevant.

## Advanced Configuration

### Parameter Storage

```properties
# Storage strategy: INLINE (default) or EXTERNAL
quarkus.jobrunr-control.parameter-storage.strategy=INLINE
# Reserved configuration key in the current runtime model
# (not actively consumed by the JDBC-based external storage implementation)
quarkus.jobrunr-control.parameter-storage.persistence-unit-name=<default>
# Reserved cleanup keys in the current runtime model
# The implemented cleanup path deletes parameter sets when the owning job is deleted
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

By default, dev mode starts **without OIDC**:

- No Keycloak instance is required
- All HTTP auth policies are set to `permit`
- `JobRunrControlRoleAugmentor` grants the `admin` role to every request

To test the real OIDC setup with the bundled example realm, start Keycloak separately and enable the `keycloak` profile:

```bash
./start-keycloak.sh
./mvnw -f jobrunr-control-example/pom.xml quarkus:dev -Dquarkus.profile=dev,keycloak
```

### Troubleshooting Dev Mode Startup

If you explicitly enable the `keycloak` profile but keep the `dev` profile active, remember that
the `%keycloak.*` overrides restore the role-based policies for `/q/jobrunr/api/*`. Without them,
the permissive `%dev.*` values would otherwise bleed into `dev,keycloak` mode.

For the authoritative configuration details, see `docs/programmers.adoc` and `docs/security.md`.

### Access Points

- JobRunr Control: `http://localhost:9090/q/jobrunr-control`
- JobRunr Dashboard: `http://localhost:9090/q/jobrunr`
- REST API in the example app: `http://localhost:9090/api/q/jobrunr-control/api`
- Swagger UI: `http://localhost:9090/q/swagger-ui/`

## License

Internal CSS Project. Requires a valid JobRunr Pro license.

## Support

For issues and questions, contact the JobRunr Control Team.

