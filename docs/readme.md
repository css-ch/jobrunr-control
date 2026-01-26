# JobRunr Control Extension

A Quarkus extension providing a web-based control dashboard and REST API for managing and monitoring jobs in JobRunr Pro.

## Overview

JobRunr Control extends the standard JobRunr Pro Dashboard with advanced scheduling and monitoring capabilities. It provides:

- **Dynamic Job Scheduling**: Configure and schedule jobs via a user-friendly web interface
- **Parameter Configuration**: Type-safe parameter input with validation for String, Integer, Boolean, Date, DateTime, and Enum types
- **Batch Job Support**: Create and monitor batch jobs with real-time progress tracking
- **External Trigger API**: REST API for external systems to trigger jobs and check status
- **Deep Link Integration**: Seamless navigation to JobRunr Pro Dashboard for detailed job analysis

## Requirements

- Java 21+
- Quarkus 3.30.8
- JobRunr Pro 8.4.1 (License required)

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>ch.css.jobrunr</groupId>
    <artifactId>jobrunr-control-extension</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure Your Application

```properties
# application.properties
quarkus.jobrunr.dashboard.type=embedded
quarkus.jobrunr.dashboard.context-path=/dashboard
```

### 3. Access the Dashboard

Start your Quarkus application and navigate to:
- **Control Dashboard**: `http://localhost:8080/q/jobrunr-control`
- **External API**: `http://localhost:8080/q/jobrunr-control/api/jobs`

## Documentation

| Document | Description |
|----------|-------------|
| [Architecture Documentation](arc42.adoc) | Technical architecture following arc42 template |
| [User Guide](user.adoc) | End-user guide for operating the dashboard |
| [Programmer's Guide](programmers.adoc) | Developer guide for implementing jobs |

## Key Features

### Role-Based Access Control (RBAC)

Three roles control access to different features:
- **viewer**: Read-only access to scheduled jobs and execution history
- **configurator**: Can create, edit, and delete scheduled jobs
- **admin**: Full access including immediate job execution

### Supported Job Types

- **Simple Jobs**: Standard one-time or scheduled jobs
- **Batch Jobs**: Jobs that process multiple items with progress tracking

### Supported Parameter Types

| Type | Format | Example |
|------|--------|---------|
| String | Plain text | `"Hello World"` |
| Integer | Whole number | `42` |
| Boolean | true/false | `true` |
| Date | ISO date | `2024-01-15` |
| DateTime | ISO datetime | `2024-01-15T10:30:00` |
| Enum | Enum constant name | `OPTION_A` |

## Development

### Build

```bash
./mvnw clean compile
```

### Run in Development Mode

```bash
./start-postgres.sh
./mvnw quarkus:dev
```

### Access Points

- Quarkus Application: http://localhost:8080
- JobRunr Control: http://localhost:8080/q/jobrunr-control
- JobRunr Dashboard: http://localhost:8000

## License

This extension requires a valid JobRunr Pro license.
