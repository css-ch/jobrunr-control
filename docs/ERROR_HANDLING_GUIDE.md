# Error Handling Developer Guide

## Quick Reference for Developers

### When to Use Which Exception

Use domain exceptions from `ch.css.jobrunr.control.domain.exceptions`:

```java
// Job not found (404)
throw new JobNotFoundException("Job with ID '"+jobId +"' not found");

// Invalid parameters (400)
throw new

ValidationException(List.of("Parameter 'x' is required", "Parameter 'y' must be positive"));

// Invalid argument (400)
        throw new

IllegalArgumentException("jobId must not be null");

// State conflict (409)
throw new

IllegalStateException("Job is already running");

// Scheduling failed (503 if transient, 500 otherwise)
throw new

JobSchedulingException("Failed to schedule job",cause);

// Execution failed (500)
throw new

JobExecutionException("Job execution failed",cause);

// Timeout (504)
throw new

TimeoutException("Operation timed out after 30 seconds");
```

### REST Endpoint Pattern

**DON'T** catch exceptions in your REST endpoints:

```java
// ❌ BAD - Don't do this
@POST
public Response createJob(CreateJobRequest request) {
    try {
        createJobUseCase.execute(request);
        return Response.ok().build();
    } catch (Exception e) {
        log.error("Error creating job", e);
        throw new InternalServerErrorException(e.getMessage());
    }
}
```

**DO** let domain exceptions propagate:

```java
// ✅ GOOD - Let GlobalExceptionMapper handle it
@POST
public Response createJob(CreateJobRequest request) {
    createJobUseCase.execute(request);
    return Response.ok().build();
}
```

### Use Case Pattern

Throw domain exceptions from use cases:

```java

@ApplicationScoped
public class CreateJobUseCase {

    public UUID execute(String jobType, String jobName, Map<String, String> parameters) {
        // Validate input
        if (jobType == null || jobType.isBlank()) {
            throw new IllegalArgumentException("jobType must not be null or blank");
        }

        // Business validation
        Optional<JobDefinition> jobDef = jobDefinitionService.findByType(jobType);
        if (jobDef.isEmpty()) {
            throw new JobNotFoundException("Job type '" + jobType + "' not found");
        }

        // Validation errors
        List<String> errors = validator.validate(parameters);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // Business logic...
        return jobSchedulerPort.scheduleJob(...);
    }
}
```

### Infrastructure Adapter Pattern

Convert infrastructure exceptions to domain exceptions:

```java
@ApplicationScoped
public class JobRunrSchedulerAdapter implements JobSchedulerPort {
    
    @Override
    public void executeJobNow(UUID jobId) {
        try {
            org.jobrunr.jobs.Job job = storageProvider.getJobById(jobId);
            
            // Not found → Domain exception
            if (job == null) {
                throw new JobNotFoundException("Job with ID '" + jobId + "' not found");
            }
            
            job.enqueue();
            storageProvider.save(job);
            
        } catch (JobNotFoundException e) {
            // Re-throw domain exceptions as-is
            throw e;
        } catch (Exception e) {
            // Wrap infrastructure exceptions in domain exception
            log.errorf(e, "Error executing job %s", jobId);
            throw new JobSchedulingException("Error executing job: " + jobId, e);
        }
    }
}
```

### Error Response Format

Clients will receive structured error responses:

**Standard Error:**

```json
{
  "errorCode": "NOT_FOUND",
  "message": "Job not found",
  "details": "Job with ID '123e4567-e89b-12d3-a456-426614174000' not found",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Validation Error:**

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed",
  "errors": [
    "Parameter 'numberOfChunks' is required",
    "Parameter 'chunkSize' must be positive"
  ],
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### Debugging

When a client reports an error:

1. Ask for the **correlation ID** from the error response
2. Search server logs for that correlation ID:
   ```bash
   grep "a1b2c3d4-e5f6-7890-abcd-ef1234567890" application.log
   ```
3. The log will contain the full stack trace and exception details

### Testing

Test your use cases with expected exceptions:

```java

@Test
void execute_NonExistentJob_ThrowsJobNotFoundException() {
    // Arrange
    UUID jobId = UUID.randomUUID();
    when(jobSchedulerPort.getJobById(jobId)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> useCase.execute(jobId))
            .isInstanceOf(JobNotFoundException.class)
            .hasMessageContaining(jobId.toString());
}
```

### Common Patterns

#### Validation with Multiple Errors

```java
List<String> errors = new ArrayList<>();

if(parameters.

get("name") ==null){
        errors.

add("Parameter 'name' is required");
}
        if(parameters.

get("count") !=null){
int count = Integer.parseInt(parameters.get("count"));
    if(count <=0){
        errors.

add("Parameter 'count' must be positive");
    }
            }

            if(!errors.

isEmpty()){
        throw new

ValidationException(errors);
}
```

#### Transient Error Detection

Infrastructure adapters can indicate transient errors:

```java
catch(TimeoutException e){
        // Transient error - client should retry
        throw new

JobSchedulingException("Service temporarily unavailable",e);
}
        catch(
SQLException e){
        if(e.

getMessage().

contains("deadlock")){
        // Transient error
        throw new

JobSchedulingException("Database deadlock, please retry",e);
    }else{
            // Permanent error
            throw new

JobSchedulingException("Database error",e);
    }
            }
```

The `GlobalExceptionMapper` will automatically return 503 (Service Unavailable) for transient errors.

## Best Practices

1. ✅ **Use specific domain exceptions** instead of generic `RuntimeException`
2. ✅ **Include context in error messages** (e.g., job ID, parameter name)
3. ✅ **Don't catch exceptions in REST endpoints** - let the mapper handle it
4. ✅ **Log before throwing** if you need debugging context
5. ✅ **Re-throw domain exceptions as-is** in infrastructure adapters
6. ✅ **Wrap infrastructure exceptions** in domain exceptions
7. ❌ **Don't expose stack traces** to clients
8. ❌ **Don't include sensitive data** in error messages (passwords, API keys, etc.)

## Migration Guide

If you have existing code with try-catch blocks in REST endpoints:

**Step 1:** Remove the try-catch wrapper:

```java
// Before
try{
        useCase.execute(...);
    return Response.

ok().

build();
}catch(
Exception e){
        throw new

InternalServerErrorException(e.getMessage());
        }

// After
        useCase.

execute(...);
return Response.

ok().

build();
```

**Step 2:** Ensure use cases throw domain exceptions:

```java
// In use case
if(jobDef.isEmpty()){
        throw new

JobNotFoundException("Job type '"+jobType +"' not found");
}
```

**Step 3:** Test and verify correct HTTP status codes

## Questions?

- See `GlobalExceptionMapper.java` for exception mapping details
- See `ARCHITECTURE_REVIEW.md` Section 4 for design rationale
- See `ERROR_HANDLING_IMPROVEMENTS.md` for implementation summary
