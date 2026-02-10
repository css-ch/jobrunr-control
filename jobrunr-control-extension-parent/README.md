# JobRunr Control Extension

Quarkus Extension for managing JobRunr Pro scheduled jobs with a web-based UI and REST API.

## Features

- 🎨 **Web UI** - Bootstrap 5 + htmx-based interface for managing scheduled jobs
- 🔌 **REST API** - External trigger endpoints for job execution
- 📊 **Job Discovery** - Automatic discovery of `ConfigurableJob` implementations
- 📝 **Job Parameters** - Type-safe parameter handling with validation
- 📈 **Execution History** - Monitor job executions and batch progress
- 🔒 **Security** - Role-based access control (viewer, configurator, admin)

## Installation

Add the extension to your Quarkus project:

```xml

<dependency>
    <groupId>ch.css.jobrunr</groupId>
    <artifactId>jobrunr-control-extension</artifactId>
    <version>1.0.0-SNAPSHOT</version>
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
jobrunr.control.ui.enabled=true
# Enable/disable REST API (default: true)
jobrunr.control.api.enabled=true
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

### 1. Implement ConfigurableJob

Create your job by implementing the `ConfigurableJob` interface:

```java

@ApplicationScoped
public class MyCustomJob implements ConfigurableJob {

    @Override
    public JobDefinition getJobDefinition() {
        return new JobDefinition(
                "my-custom-job",
                "My Custom Job",
                "Description of what this job does",
                List.of(
                        new JobParameter("param1", JobParameterType.STRING, "Parameter 1", true),
                        new JobParameter("param2", JobParameterType.INTEGER, "Parameter 2", false)
                ),
                false // not a batch job
        );
    }

    @Job(name = "My Custom Job")
    public void execute(MyCustomJobRequest request) {
        // Your job logic here
        System.out.println("Processing: " + request.param1());
    }
}
```

### 2. Create Job Request

```java
public record MyCustomJobRequest(
        @NotNull String param1,
        Integer param2
) implements JobRequest {

    @Override
    public Class<MyCustomJob> getJobClass() {
        return MyCustomJob.class;
    }
}
```

### 3. Access the UI

Navigate to `http://localhost:8080/` (or your configured root path) to:

- View all discovered jobs
- Schedule jobs with parameters
- Monitor execution history
- Track batch job progress

### 4. Use REST API

Trigger jobs externally:

```bash
# Trigger a job
curl -X POST http://localhost:8080/api/external-trigger/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "jobDefinitionId": "my-custom-job",
    "parameters": {
      "param1": "value1",
      "param2": 42
    }
  }'

# Check batch progress
curl http://localhost:8080/api/external-trigger/batch/abc-123/progress
```

## UI Endpoints

| Path              | Description                             |
|-------------------|-----------------------------------------|
| `/`               | Dashboard (redirects to scheduled jobs) |
| `/scheduled-jobs` | List all scheduled jobs                 |
| `/executions`     | View execution history                  |

## REST API Endpoints

| Method | Path                      | Roles Required                        | Description                 |
|--------|---------------------------|---------------------------------------|-----------------------------|
| POST   | `/api/jobs/{jobId}/start` | `api-executor`, `admin`               | Start a job immediately     |
| GET    | `/api/jobs/{jobId}`       | `api-reader`, `api-executor`, `admin` | Get job status and progress |

**Note:** All REST API endpoints require authentication and appropriate role assignment.

## Security

The extension uses role-based access control (RBAC):

**UI Roles:**

- **viewer**: Read-only access to web UI
- **configurator**: Can schedule and delete jobs via web UI
- **admin**: Full access to web UI and all API endpoints

**API Roles:**

- **api-reader**: Read-only access to REST API (GET operations)
- **api-executor**: Can execute jobs via REST API (POST operations)

Configure roles in your Quarkus security setup (e.g., OIDC, LDAP, database).

### Dev Mode

For development and testing, you can configure which roles are automatically granted:

```properties
# Default includes all roles for easy testing
dev.test.roles=viewer,configurator,admin,api-reader,api-executor
```

## Architecture

The extension follows Clean Architecture principles:

```
runtime/
├── domain/           # Core models and ports
├── application/      # Use cases
├── infrastructure/   # JobRunr integration
├── adapter/
│   ├── rest/        # REST API
│   └── ui/          # UI controllers
└── dev/             # Dev mode features
```

## Advanced Configuration

### Parameter Storage

Configure parameter storage strategy and persistence:

```properties
# Storage strategy: INLINE (default) or EXTERNAL
jobrunr.control.parameter-storage.strategy=INLINE
# Persistence unit for external parameter storage (build-time config)
# Default: <default> (Hibernate ORM default persistence unit)
jobrunr.control.parameter-storage.persistence-unit-name=<default>
# Cleanup configuration for external parameter storage
jobrunr.control.parameter-storage.cleanup.enabled=true
jobrunr.control.parameter-storage.cleanup.retention-days=30
```

**Note:** The `persistence-unit-name` is a build-time configuration that determines which Hibernate ORM persistence unit
is used for storing external parameter sets. If you have multiple persistence units configured, you can specify which
one to use for JobRunr Control's parameter storage.

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

### Requirements

- Java 21+
- Quarkus 3.30.6
- JobRunr Pro 8.4.2
- PostgreSQL (or H2 for dev/test)

## License

Internal CSS Project

## Support

For issues and questions, contact the JobRunr Control Team.
