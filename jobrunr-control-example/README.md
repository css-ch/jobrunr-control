# JobRunr Control - Example Application

Example application demonstrating the usage of the `jobrunr-control-extension`.

## Overview

This application showcases:

- How to use the JobRunr Control Extension
- Example job implementations
- Configuration examples
- Integration tests

## Jobs Included

### 1. NotificationJob

Sends notifications (email, SMS, etc.)

**Parameters:**

- `recipient` (String, required)
- `message` (String, required)
- `channel` (String, required) - email, sms, push

### 2. ParameterDemoJob

Demonstrates all parameter types

**Parameters:**

- `stringParam` (String)
- `integerParam` (Integer)
- `booleanParam` (Boolean)
- `instantParam` (Instant)

### 3. SimpleReportJob

Generates reports

**Parameters:**

- `reportType` (String, required)
- `startDate` (Instant, required)
- `endDate` (Instant, required)
- `format` (String) - PDF, CSV, EXCEL

### 4. SystemMaintenanceJob

Performs system maintenance tasks

**Parameters:**

- `taskType` (String, required) - cleanup, backup, optimize
- `dryRun` (Boolean) - preview changes without applying

### 5. CalculationBatchJob

Batch job for processing calculations

**Parameters:**

- `batchSize` (Integer, required)
- `totalItems` (Integer, required)

## Running the Application

### Development Mode

```bash
cd jobrunr-control-example
mvn quarkus:dev
```

Access the UI at: http://localhost:8080

### Production Build

```bash
mvn package
java -jar target/quarkus-app/quarkus-run.jar
```

### Native Build

```bash
mvn package -Pnative
./target/jobrunr-control-example-1.0.0-SNAPSHOT-runner
```

## Configuration

See `src/main/resources/application.properties` for all configuration options.

### Key Settings

```properties
# Extension
jobrunr.control.ui.enabled=true
jobrunr.control.api.enabled=true
# JobRunr
quarkus.jobrunr.background-job-server.enabled=true
quarkus.jobrunr.dashboard.enabled=true
quarkus.jobrunr.dashboard.port=8000
# Database
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/postgres
```

### Dev Profile

Development mode uses H2 in-memory database:

```properties
%dev.quarkus.datasource.db-kind=h2
%dev.quarkus.datasource.jdbc.url=jdbc:h2:mem:test
```

## Database Setup

### PostgreSQL

```bash
# Start PostgreSQL with Docker/Podman
./start-postgres.sh

# Or manually
docker run -d \
  --name jobrunr-postgres \
  -e POSTGRES_PASSWORD=your_strong_password \
  -p 5432:5432 \
  postgres:16
```

### H2 (Dev Mode)

No setup required - automatically created in memory.

## Testing

### Run Tests

```bash
mvn test
```

### Integration Tests

The example includes integration tests for:

- Job scheduling
- Job execution
- Parameter validation
- Batch job processing

See `src/test/java/ch/css/jobrunr/example/infrastructure/scheduler/`

## Adding Your Own Jobs

1. Create a job class implementing `ConfigurableJob`:

```java
package com.mycompany.jobs;

@ApplicationScoped
public class MyJob implements ConfigurableJob {

    @Override
    public JobDefinition getJobDefinition() {
        return new JobDefinition(
                "my-job",
                "My Job",
                "What it does",
                List.of(/* parameters */),
                false
        );
    }

    @Job(name = "My Job")
    public void execute(MyJobRequest request) {
        // Implementation
    }
}
```

2. Create the request class:

```java
public record MyJobRequest(
        String param1
) implements JobRequest {

    @Override
    public Class<MyJob> getJobClass() {
        return MyJob.class;
    }
}
```

3. The extension will automatically discover your job!

## Project Structure

```
src/
├── main/
│   ├── java/ch/css/jobrunr/example/
│   │   └── jobs/              # All example jobs
│   │       ├── NotificationJob.java
│   │       ├── ParameterDemoJob.java
│   │       ├── SimpleReportJob.java
│   │       ├── SystemMaintenanceJob.java
│   │       └── calculation/   # Batch job example
│   └── resources/
│       └── application.properties
└── test/
    └── java/                  # Integration tests
```

## Troubleshooting

### Jobs not appearing in UI

- Check that your job implements `ConfigurableJob`
- Ensure the job class is `@ApplicationScoped`
- Verify package scanning is correct
- Check application logs for discovery errors

### Database connection issues

- Verify PostgreSQL is running: `docker ps`
- Check connection string in `application.properties`
- Ensure database credentials are correct

### JobRunr Dashboard

Access JobRunr Pro dashboard at: http://localhost:8000

## Links

- Extension README: `../jobrunr-control-extension-parent/README.md`
- Migration Documentation: `../docs/README-MIGRATION.md`
- Technical Spec: `../.spec/technical-spec.md`

## License

Internal CSS Project
