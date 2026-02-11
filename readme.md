# Quarkus - JobRunr Control

A Quarkus extension providing a web-based control dashboard and REST API for managing and monitoring jobs in JobRunr
Pro.

## Overview

JobRunr Control extends the standard JobRunr Pro Dashboard with advanced scheduling and monitoring capabilities. It
provides:

- **Dynamic Job Scheduling**: Configure and schedule jobs via a user-friendly web interface
- **Parameter Configuration**: Type-safe parameter input with validation for String, Integer, Boolean, Date, DateTime,
  and Enum types
- **Batch Job Support**: Create and monitor batch jobs with real-time progress tracking
- **External Trigger API**: REST API for external systems to trigger jobs and check status
- **Deep Link Integration**: Seamless navigation to JobRunr Pro Dashboard for detailed job analysis

## Requirements

- Java 21+
- Quarkus 3.31.2
- JobRunr Pro 8.4.2 (License required)

## Quick Start

### 1. Add Dependency

```xml

<dependency>
    <groupId>ch.css.quarkus</groupId>
    <artifactId>quarkus-jobrunr-control</artifactId>
    <version>1.0.1</version>
</dependency>

        ### 2. Configure Your Application

        ```properties
        # application.properties
        quarkus.jobrunr.dashboard.type=embedded
        quarkus.jobrunr.dashboard.context-path=/dashboard
        # Enable dashboard access in dev mode (disable authentication)
        %dev.quarkus.jobrunr.dashboard.security.allow-all-to-be-monitored=true
```

### 3. Access the Dashboard

Start your Quarkus application and navigate to:

- **Control Dashboard**: `http://localhost:8080/q/jobrunr-control` (Your custom UI)
- **JobRunr Dashboard**: `http://localhost:8080/q/jobrunr/dashboard` (JobRunr Pro monitoring)
- **External API**: `http://localhost:8080/q/swagger-ui/`

> **Note**: The JobRunr Pro Dashboard requires security configuration.
> See [Dashboard Access Guide](docs/JOBRUNR_DASHBOARD_ACCESS.md) for details.

## Documentation

| Document                                                   | Description                                                            |
|------------------------------------------------------------|------------------------------------------------------------------------|
| [Architecture Documentation](docs/arc42.adoc)              | Technical architecture following arc42 template                        |
| [User Guide](docs/user.adoc)                               | End-user guide for operating the dashboard                             |
| [Programmer's Guide](docs/programmers.adoc)                | Developer guide for implementing jobs                                  |
| [Dashboard Access Guide](docs/JOBRUNR_DASHBOARD_ACCESS.md) | Configuring access to JobRunr Pro Dashboard                            |
| [Error Handling Guide](docs/ERROR_HANDLING_GUIDE.md)       | Error handling patterns and best practices                             |
| [Contributor Guidance](docs/copilot-instructions.adoc)     | Copilot/contributor instructions and logging/documentation conventions |

## Key Features

### Role-Based Access Control (RBAC)

Five roles control access to different features:

**UI Roles:**

- **viewer**: Read-only access to scheduled jobs and execution history via web UI
- **configurator**: Can create, edit, and delete scheduled jobs via web UI
- **admin**: Full access including immediate job execution via web UI

**API Roles:**

- **api-reader**: Read-only access to REST API endpoints (GET operations)
- **api-executor**: Can execute jobs via REST API (POST operations)

**Note:** The `admin` role has access to both UI and API endpoints.

### Supported Job Types

- **Simple Jobs**: Standard one-time or scheduled jobs
- **Batch Jobs**: Jobs that process multiple items with progress tracking

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

The extension supports two parameter storage strategies:

- **Inline Storage** (default): Parameters stored directly in JobRunr's job table
- **External Storage**: Parameters stored in separate database table for large parameter sets

Use `@JobParameterSet` annotation on JobRequest to enable external storage. Requires Hibernate ORM configuration.

### Job Chain Status Evaluation

For jobs with continuation chains (using `continueWith()` or `onFailure()`), the extension automatically evaluates the
overall status of the entire chain:

- **Chain Status Determination**: The status reflects the state of all jobs in the chain, not just the parent job
- **Completion Detection**: A chain is complete when all relevant leaf jobs have finished (SUCCEEDED, FAILED, or
  DELETED)
- **In-Progress Tracking**: A chain is IN_PROGRESS when any leaf job is still running (ENQUEUED, PROCESSING, PROCESSED)
- **Success Evaluation**: A chain SUCCEEDED when all executed leaf jobs succeeded
- **Failure Evaluation**: A chain FAILED when any executed leaf job failed

This ensures accurate status reporting for complex job workflows with multiple continuation steps.

## Development

### Build

```bash
./mvnw clean compile
```

### Run in Development Mode

```bash
./mvnw -f jobrunr-control-example/pom.xml quarkus:dev
```

### Access Points

- Quarkus Application: http://localhost:8080
- JobRunr Control: http://localhost:8080/q/jobrunr-control
- JobRunr Dashboard: http://localhost:8080/q/jobrunr/dashboard

## Release Management

For information on how to release this extension to Maven Central, see the [Release Documentation](docs/RELEASE.md).

## License

This extension requires a valid JobRunr Pro license.
