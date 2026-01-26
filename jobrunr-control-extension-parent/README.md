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

- JobRunr Pro 8.4.0
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

| Method | Path                                             | Description        |
|--------|--------------------------------------------------|--------------------|
| POST   | `/api/external-trigger/trigger`                  | Trigger a job      |
| GET    | `/api/external-trigger/batch/{batchId}/progress` | Get batch progress |

## Security

The extension uses role-based access control:

- **viewer**: Read-only access to UI
- **configurator**: Can schedule and delete jobs
- **admin**: Full access

Configure roles in your Quarkus security setup.

### Dev Mode

For development, you can configure test roles:

```properties
dev.test.roles=admin
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

## Requirements

- Java 21+
- Quarkus 3.30.6
- JobRunr Pro 8.4.0
- PostgreSQL (or H2 for dev/test)

## License

Internal CSS Project

## Support

For issues and questions, contact the JobRunr Control Team.
