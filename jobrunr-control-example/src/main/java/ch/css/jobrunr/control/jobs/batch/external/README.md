# Data Migration Batch Job - External Parameter Storage Example

## Overview

This package contains a complete example of a batch job using **external parameter storage** via the `@JobParameterSet`
annotation. It demonstrates how to store large parameter sets externally in the database instead of inline in the
JobRunr job metadata.

## Components

### 1. DataMigrationBatchJobRequest

The main job request record that uses `@JobParameterSet` to store parameters externally.

**External Parameters:**

- `sourceName` (String) - Source system name (default: "legacy_system")
- `targetName` (String) - Target system name (default: "new_system")
- `numberOfBatches` (Integer) - Number of batches to process (default: 50)
- `batchSize` (Integer) - Number of records per batch (default: 100)
- `simulateErrors` (Boolean) - Enable error simulation (default: false)
- `migrationDate` (LocalDate) - Migration date (default: "2026-01-01")

**Key Feature:** Only the `parameterSetId` (UUID) is stored in the job metadata. All actual parameters are stored in the
`jobrunr_control_parameter_sets` table.

### 2. DataMigrationBatchJob

The batch job handler that:

1. Loads parameters from external storage using `ParameterStorageService`
2. Creates batch items based on the parameters
3. Enqueues background jobs for parallel processing
4. Saves metadata about the migration

### 3. DataMigrationBatchItemRequest

Individual batch item request containing:

- Batch ID and size
- Source and target system names
- Migration date
- Error simulation flag

### 4. DataMigrationBatchItemProcessor

Processes individual batch items:

- Simulates reading records from source system
- Simulates writing records to target system
- Tracks success/error counts
- Saves statistics as job metadata
- Handles errors gracefully with retry logic

## Benefits of External Parameter Storage

### Why Use @JobParameterSet?

1. **Large Parameter Sets**: Keeps JobRunr job table lean when parameters are large
2. **Complex Objects**: Better handling of nested or complex parameter structures
3. **Separate Queries**: Parameters can be queried independently from jobs
4. **History Analysis**: External parameters enriched in history display automatically

### Comparison with Inline Parameters

**Inline (ExampleBatchJob):**

```java
public record ExampleBatchJobRequest(
        @JobParameterDefinition(defaultValue = "100") Integer numberOfChunks,
        Integer chunkSize,
        @JobParameterDefinition(defaultValue = "true") Boolean simulateErrors
) implements JobRequest { ...
}
```

✅ Simple and direct
❌ Parameters stored in JobRunr job metadata (can bloat table)

**External (DataMigrationBatchJob):**

```java
public record DataMigrationBatchJobRequest(
        @JobParameterSet({
                @JobParameterDefinition(name = "numberOfBatches", type = "java.lang.Integer", defaultValue = "50"),
                @JobParameterDefinition(name = "batchSize", type = "java.lang.Integer", defaultValue = "100"),
                // ... more parameters
        })
        String parameterSetId
) implements JobRequest { ...
}
```

✅ Lean job metadata (only UUID stored)
✅ Supports large/complex parameters
✅ Parameters stored in separate table
❌ Requires Hibernate ORM to be enabled

## How to Use

### Prerequisites

Ensure Hibernate ORM is enabled in your profile:

```properties
# application.properties
%dev.quarkus.hibernate-orm.enabled=true
%dev.quarkus.datasource.db-kind=postgresql
%dev.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/yourdb
```

### Scheduling a Job

Via JobRunr Control UI:

1. Navigate to http://localhost:8080/q/jobrunr-control
2. Click "Schedule New Job"
3. Select "DataMigrationBatchJob"
4. Fill in parameters (or use defaults)
5. Click "Schedule"

Via API:

```bash
curl -X POST http://localhost:8080/q/jobrunr-control/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "DataMigrationBatchJob",
    "jobName": "Migration: Legacy to New System",
    "parameters": {
      "sourceName": "legacy_db",
      "targetName": "new_db",
      "numberOfBatches": "20",
      "batchSize": "50",
      "simulateErrors": "false",
      "migrationDate": "2026-02-01"
    },
    "isExternalTrigger": false,
    "scheduledAt": "2026-01-29T12:00:00Z"
  }'
```

### Monitoring

1. **JobRunr Dashboard**: http://localhost:8080/q/jobrunr
    - View batch progress
    - See individual item jobs
    - Check for failures

2. **JobRunr Control History**: http://localhost:8080/q/jobrunr-control/history
    - Parameters automatically loaded from external storage
    - Full parameter set displayed (not just the ID)

3. **Database**:

```sql
-- View parameter sets
SELECT id, job_type, created_at, last_accessed_at
FROM jobrunr_control_parameter_sets;

-- View parameters for specific job
SELECT parameters_json
FROM jobrunr_control_parameter_sets
WHERE id = 'your-uuid-here';
```

## Implementation Details

### Parameter Loading

```java
// In DataMigrationBatchJob.run()
UUID parameterSetId = UUID.fromString(request.parameterSetId());
ParameterSet parameterSet = parameterStorageService.findById(parameterSetId)
        .orElseThrow(() -> new IllegalStateException("Parameter set not found"));

String sourceName = (String) parameterSet.parameters().get("sourceName");
Integer numberOfBatches = (Integer) parameterSet.parameters().get("numberOfBatches");
// ... extract other parameters
```

### Error Handling

- Batch items retry up to 2 times (`@Job(retries = 2)`)
- Individual record errors logged but don't fail entire batch
- Batch fails if >10% of records fail
- Success/error counts saved as metadata

### Performance Characteristics

- Preparation: ~3 seconds (simulated connection time)
- Per record: ~100ms (50ms read + 50ms write)
- Default batch: 50 batches × 100 records = 5000 records total
- Estimated total time: ~8-10 minutes with parallel processing

## Testing Scenarios

### 1. Happy Path

```properties
numberOfBatches=10
batchSize=50
simulateErrors=false
```

Expected: All 500 records migrate successfully

### 2. With Errors

```properties
numberOfBatches=20
batchSize=100
simulateErrors=true
```

Expected: ~1% error rate, some batches may retry, most succeed

### 3. Large Migration

```properties
numberOfBatches=100
batchSize=200
simulateErrors=false
```

Expected: 20,000 records, tests system under load

## Comparison with ExampleBatchJob

| Feature                | ExampleBatchJob | DataMigrationBatchJob         |
|------------------------|-----------------|-------------------------------|
| Parameter Storage      | Inline          | External                      |
| Parameter Count        | 3               | 6                             |
| Requires Hibernate ORM | No              | Yes                           |
| Suitable for           | Simple jobs     | Complex jobs with many params |
| History Display        | Direct          | Enriched from DB              |
| Job Metadata Size      | Medium          | Small (only UUID)             |

## Next Steps

1. **Extend**: Add more parameters (e.g., transformation rules, filtering criteria)
2. **Real Integration**: Connect to actual source/target systems
3. **Monitoring**: Add custom metrics and dashboards
4. **Cleanup**: Implement parameter set cleanup after job completion
5. **Validation**: Add parameter validation in the job handler

## See Also

- [QUICKSTART_EXTERNAL_PARAMS.md](../../../QUICKSTART_EXTERNAL_PARAMS.md) - Quick start guide
- [plan_parameterset.md](../../../plan_parameterset.md) - Implementation plan
- [IMPLEMENTATION_COMPLETE.md](../../../IMPLEMENTATION_COMPLETE.md) - Implementation summary
