# Implementation Plan: ParameterSetEntity UUID Alignment with Job UUID

## Summary

Currently, `ParameterSetEntity` uses a randomly generated UUID as its ID (`UUID parameterSetId = UUID.randomUUID()` in
`ParameterStorageHelper`). The requirement is to change this so that:

1. **The ParameterSetEntity UUID should match the Job UUID** to which the parameter set belongs
2. **When a template is started (executed)**, the parameters should be copied with a new UUID (since a new job is
   created)
3. **When createOrUpdate creates a new UUID**, the old ParameterSetEntity should be deleted (cleanup)

## Key Implementation Approach

**Critical Insight:** JobRunr's `JobScheduler.createOrReplace()` **persists the job immediately** and returns the actual
job UUID. This means we can:

1. **Create the job first** (with inline parameters or empty params)
2. **Get the actual job UUID** from the return value
3. **Store parameters using that UUID** as the parameter set ID
4. **Update the job** to reference the parameter set

This eliminates the need to pre-generate UUIDs and hope they match. It's simpler, more reliable, and follows a natural
two-phase creation pattern.

## Key Architectural Note

**The parameter set field (e.g., `parameterSetId`) is still required in JobRequest records**, but its purpose changes:

- **Before**: Field contains a random UUID that references a separate parameter set
- **After**: Field contains the job's own UUID (which is also the parameter set ID)
- **Why it's still needed**: JobRunr needs to deserialize the JobRequest, so the field must exist and be populated

This means:

- `JobDefinition.parameterSetFieldName()` is still used to identify which field to populate
- We still pass a map like `Map.of(parameterSetFieldName, jobId.toString())` to JobRunr
- But now `jobId == parameterSetId` (they're the same UUID)

**Important:** Jobs with **inline parameters** (not using `@JobParameterSet`) are **completely unchanged**. The
two-phase
creation only applies to jobs with external parameters. Inline parameter jobs work exactly as before.

## Current Architecture Analysis

### Parameter Storage Flow

1. **Creating/Updating Jobs:**
    - `CreateScheduledJobUseCase` / `UpdateScheduledJobUseCase` / `CreateTemplateUseCase` / `UpdateTemplateUseCase`
    - All use `ParameterStorageHelper.prepareJobParameters()` which:
        - Generates `UUID parameterSetId = UUID.randomUUID()` (currently random - **WILL BE CHANGED TO JOB UUID**)
        - Stores parameters with this ID via `parameterStorageService.store(parameterSet)`
        - Returns a map with the parameter set field (e.g., `Map.of(parameterSetFieldName, jobId.toString())`) to be
          stored in the job
    - **Note**: The parameter set field name (e.g., `parameterSetId`) is still needed in the JobRequest, but it will now
      contain the job UUID instead of a random UUID

2. **Job Scheduling:**
    - `JobRunrSchedulerAdapter.createOrUpdateJob()` receives the job parameters map
    - Calls `JobInvoker.scheduleJob()` with optional `UUID jobId` parameter
    - `JobBuilder.withId(jobId)` - if jobId is null, JobRunr generates new UUID
    - Returns the actual job UUID

3. **Template Cloning:**
    - `TemplateCloneHelper.cloneTemplate()` clones templates and creates new jobs
    - Always creates new jobs (jobId = null), which get new UUIDs
    - Currently doesn't handle parameter copying explicitly

4. **Job Update:**
    - `updateJob()` calls `createOrUpdateJob()` with existing jobId
    - JobRunr's `createOrReplace()` can update or create based on jobId
    - If jobId changes, old job is effectively replaced

## Problem Breakdown

### Issue 1: ParameterSetEntity ID ≠ Job ID

**Current:** Random UUID for parameter sets  
**Required:** Parameter set ID = Job ID

**Key Insight:** The job is already persisted by `JobScheduler.createOrReplace()` before we return from the scheduling
call, so we can use the returned job UUID to store parameters **after** job creation. No need to pre-generate UUIDs.

**Impact Areas:**

- `ParameterStorageHelper` - needs methods to store parameters **after** job creation
- Job creation flow - create job first, then store parameters with returned UUID
- Job update flow - delete old parameters, create job, store new parameters

### Issue 2: Template Execution Doesn't Copy Parameters

**Current:** Template execution clones job, but parameter set is referenced by ID (shared between template and executed
jobs)  
**Required:** New parameter set with new UUID when template is executed (each execution gets its own parameter copy)

**Impact Areas:**

- `TemplateCloneHelper.cloneTemplate()`
- `ExecuteTemplateUseCase`
- Parameter copying logic after job creation

### Issue 3: Parameter Loading Can Be Simplified

**Current:** `JobRunrParameterSetLoaderAdapter.loadParameters(jobId)` loads the job from JobRunr, extracts the parameter
set ID from the job's parameters, then loads the parameter set.

**After Change:** Since `jobId == parameterSetId`, we can directly load the parameter set using the job ID.

**Impact Areas:**

- `JobRunrParameterSetLoaderAdapter` - simplify loading logic
- `ParameterSetLoaderPort` - may simplify interface

### Issue 4: No Cleanup of Old Parameter Sets on Update

**Current:** When job is updated/replaced with new UUID, old parameter set remains  
**Required:** Delete old parameter set when job UUID changes

**Impact Areas:**

- `UpdateScheduledJobUseCase`
- `UpdateTemplateUseCase`
- Job replacement logic

## Implementation Plan

### Phase 1: Change Parameter Storage to Use Job UUID

#### 1.1 Modify `ParameterStorageHelper`

**File:**
`jobrunr-control-extension-parent/runtime/src/main/java/ch/css/jobrunr/control/application/scheduling/ParameterStorageHelper.java`

**Changes:**

- Add new method
  `storeParametersForJob(UUID jobId, JobDefinition jobDefinition, String jobType, Map<String, Object> convertedParameters)`
  to store parameters **after** job creation
- Use provided `jobId` (from JobRunr) as the parameter set ID
- Keep existing `prepareJobParameters()` for inline parameters (unchanged)

**Example:**

```java
/**
 * Stores parameters for a job that has already been created.
 * Used for jobs with external parameter storage after the job UUID is known.
 *
 * @param jobId The UUID of the already-created job
 * @param jobDefinition The job definition
 * @param jobType The job type (for storage identification)
 * @param convertedParameters The validated and converted parameters
 */
public void storeParametersForJob(
        UUID jobId,
        JobDefinition jobDefinition,
        String jobType,
        Map<String, Object> convertedParameters) {

    if (!jobDefinition.usesExternalParameters()) {
        return; // Nothing to store
    }

    if (!parameterStorageService.isExternalStorageAvailable()) {
        throw new IllegalStateException(
                "Job '" + jobType + "' requires external parameter storage (@JobParameterSet), " +
                        "but external storage is not configured. " +
                        "Enable Hibernate ORM: quarkus.hibernate-orm.enabled=true");
    }

    // Use job UUID as parameter set ID
    ParameterSet parameterSet = ParameterSet.create(jobId, jobType, convertedParameters);
    parameterStorageService.store(parameterSet);

    LOG.infof("Stored parameters externally with ID: %s (job UUID) for job type: %s", jobId, jobType);
}

/**
 * Prepares a reference map to the parameter set for jobs with external parameters.
 * Returns empty map for inline parameters, or a map with the parameter set field reference.
 *
 * @param jobId The job UUID (used as parameter set ID reference)
 * @param jobDefinition The job definition
 * @return Map with parameter set reference, or empty map for inline parameters
 */
public Map<String, Object> createParameterReference(UUID jobId, JobDefinition jobDefinition) {
    if (!jobDefinition.usesExternalParameters()) {
        return Map.of(); // Empty for inline parameters
    }

    return Map.of(jobDefinition.parameterSetFieldName(), jobId.toString());
}
```

#### 1.2 Modify Job Creation Flow

**Approach:** Create job first with inline/empty parameters, get the UUID back, then store external parameters.

**Files to Modify:**

- `CreateScheduledJobUseCase.execute()`
- `CreateTemplateUseCase.execute()`

**Changes:**

```java
// Validate and convert parameters
Map<String, Object> convertedParameters = validator.convertAndValidate(jobDefinition, parameters);

// For jobs with external params, pass empty params initially; for inline params use converted parameters
Map<String, Object> initialParams = jobDefinition.usesExternalParameters()
        ? Map.of() // Empty initially - will store externally and update
        : convertedParameters; // Inline params - pass directly

// Create job - JobRunr persists it and returns the actual UUID
UUID jobId = jobSchedulerPort.scheduleJob(
        jobDefinition, jobName, initialParams,
        isExternalTrigger, effectiveScheduledAt, labels);

// Store external parameters with job UUID and update job
if(jobDefinition.

usesExternalParameters()){
        parameterStorageHelper.

storeParametersForJob(jobId, jobDefinition, jobType, convertedParameters);

Map<String, Object> paramReference = parameterStorageHelper.createParameterReference(jobId, jobDefinition);
    jobSchedulerPort.

updateJobParameters(jobId, paramReference);
    
    LOG.

infof("Stored and linked external parameters for job: %s",jobId);
}

        return jobId;
```

#### 1.3 Modify Job Update Flow

**Files to Modify:**

- `UpdateScheduledJobUseCase.execute()`
- `UpdateTemplateUseCase.execute()`

**Changes:**

For updates, the job UUID stays the same. We need to:

1. Delete old parameter set (if exists)
2. Update the job
3. Store new parameter set with same job UUID

```java
// Validate and convert parameters
Map<String, Object> convertedParameters = validator.convertAndValidate(jobDefinition, parameters);

// Delete old parameter set if job uses external parameters
if(jobDefinition.

usesExternalParameters()){
        parameterStorageService.

deleteById(jobId);
    LOG.

debugf("Deleted old parameter set for job: %s",jobId);
}

// Determine parameters to pass to job update
Map<String, Object> jobParameters = jobDefinition.usesExternalParameters()
        ? Map.of() // Empty for external params (will update after)
        : convertedParameters; // Inline params

// Update job
jobSchedulerPort.

updateJob(jobId, jobDefinition, jobName, jobParameters,
          isExternalTrigger, effectiveScheduledAt, additionalLabels);

// Store new parameters with same job UUID
if(jobDefinition.

usesExternalParameters()){
        parameterStorageHelper.

storeParametersForJob(jobId, jobDefinition, jobType, convertedParameters);

// Update job with parameter reference
Map<String, Object> paramReference = parameterStorageHelper.createParameterReference(jobId, jobDefinition);
    jobSchedulerPort.

updateJobParameters(jobId, paramReference);
    
    LOG.

infof("Updated external parameters for job: %s",jobId);
}
```

### Phase 2: Handle Template Execution with Parameter Copying

#### 2.1 Modify `TemplateCloneHelper`

**File:**
`jobrunr-control-extension-parent/runtime/src/main/java/ch/css/jobrunr/control/application/template/TemplateCloneHelper.java`

**Changes:**
Add parameter copying logic for templates with external parameters:

```java
public UUID cloneTemplate(UUID templateId, String postfix,
                          Map<String, Object> parameterOverrides,
                          List<String> additionalLabels) {
    // ... existing code to get sourceJob ...

    // Check if source job uses external parameters
    boolean usesExternalParams = sourceJob.jobDefinition().usesExternalParameters();

    Map<String, Object> initialParameters;
    Map<String, Object> actualParameters = null;

    if (usesExternalParams && parameterStorageService.isExternalStorageAvailable()) {
        // Load original parameters from external storage
        String paramSetIdStr = (String) sourceJob.parameters().get(
                sourceJob.jobDefinition().parameterSetFieldName());
        UUID sourceParamSetId = UUID.fromString(paramSetIdStr);

        Optional<ParameterSet> sourceParamSet = parameterStorageService.findById(sourceParamSetId);
        if (sourceParamSet.isEmpty()) {
            throw new IllegalStateException("Source parameter set not found: " + sourceParamSetId);
        }

        // Merge actual parameters (not the reference)
        actualParameters = new HashMap<>(sourceParamSet.get().parameters());
        if (parameterOverrides != null && !parameterOverrides.isEmpty()) {
            actualParameters.putAll(parameterOverrides);
            LOG.infof("Applied %s parameter override(s)", parameterOverrides.size());
        }

        // Create job with empty params initially (will update after)
        initialParameters = Map.of();
    } else {
        // Inline parameters - existing logic
        initialParameters = new HashMap<>(sourceJob.parameters());
        if (parameterOverrides != null && !parameterOverrides.isEmpty()) {
            initialParameters.putAll(parameterOverrides);
        }
    }

    // Generate new job name
    String newJobName = generateJobName(sourceJob.jobName(), postfix);

    // Schedule the new job - let JobRunr generate the UUID
    UUID newJobId = jobSchedulerPort.scheduleJob(
            sourceJob.jobDefinition(),
            newJobName,
            initialParameters,
            true,
            null,
            additionalLabels
    );

    // If external params, store them now with the actual job UUID
    if (usesExternalParams && actualParameters != null) {
        parameterStorageHelper.storeParametersForJob(
                newJobId,
                sourceJob.jobDefinition(),
                sourceJob.jobDefinition().jobType(),
                actualParameters
        );

        // Update job with parameter reference
        Map<String, Object> paramReference = parameterStorageHelper.createParameterReference(
                newJobId,
                sourceJob.jobDefinition()
        );
        jobSchedulerPort.updateJobParameters(newJobId, paramReference);

        LOG.infof("Copied parameter set to new job: %s", newJobId);
    }

    return newJobId;
}
```

**Dependencies:**

- Need to inject `ParameterStorageService` and `ParameterStorageHelper` into `TemplateCloneHelper`
- Need `JobSchedulerPort.updateJobParameters()` method for updating just the parameters

#### 2.2 Modify `JobSchedulerPort` Interface

**File:** `jobrunr-control-extension-parent/runtime/src/main/java/ch/css/jobrunr/control/domain/JobSchedulerPort.java`

**Changes:**
Add method to update only job parameters (for linking parameter sets after job creation):

```java
/**
 * Updates only the parameters of an existing job.
 * Used to link external parameter sets after job creation.
 *
 * @param jobId The job UUID
 * @param parameters The new parameters map (typically just a parameter set reference)
 */
void updateJobParameters(UUID jobId, Map<String, Object> parameters);
```

**Implementation in `JobRunrSchedulerAdapter`:**

```java

@Override
public void updateJobParameters(UUID jobId, Map<String, Object> parameters) {
    try {
        var job = storageProvider.getJobById(JobId.asUUID(jobId));

        // Get current JobRequest
        JobRequest currentJobRequest = job.getJobRequest();

        // Merge new parameters into current JobRequest
        // Note: This requires reflection or JobRunr's update mechanism
        // May need to use createOrReplace with existing jobId instead
        JobRequest updatedJobRequest = mergeParameters(currentJobRequest, parameters);
        job.setJobRequest(updatedJobRequest);

        storageProvider.save(job);

        LOG.debugf("Updated parameters for job: %s", jobId);
    } catch (Exception e) {
        LOG.errorf(e, "Error updating job parameters: %s", jobId);
        throw new JobSchedulingException("Error updating job parameters: " + jobId, e);
    }
}

/**
 * Merges new parameters into existing JobRequest.
 * Implementation TBD - may need to use reflection or recreate JobRequest with updated field.
 */
private JobRequest mergeParameters(JobRequest currentRequest, Map<String, Object> newParameters) {
    // TODO: Implement proper merge logic
    // Option 1: Use reflection to update the parameterSetId field
    // Option 2: Convert to map, merge, convert back
    // Option 3: Use JobRunr's createOrReplace mechanism instead
    throw new UnsupportedOperationException("Implementation needed");
}
```

**Note:** The implementation of parameter merging needs investigation. Alternative approach is to use
`createOrReplace()`
with the existing job ID instead of direct parameter update.

### Phase 3: Cleanup Old Parameters on Job Replacement

#### 3.1 Detect When Job UUID Changes

**Scenario:** When `updateJob()` is called with a jobId, but JobRunr decides to create a new UUID.

**Reality:** This should be extremely rare since JobRunr's `createOrReplace()` respects the provided UUID. However, we
should handle it for robustness.

**Solution:**

- The `updateJob()` method in `JobSchedulerPort` should return the actual job UUID
- Compare input jobId with returned jobId
- If different, delete old parameter set

#### 3.2 Add Cleanup Logic

**File:** Various Use Cases that update jobs

**Note:** With the new approach, this scenario is extremely unlikely since we control the UUID. However, for defensive
programming:

**Pattern:**

```java
UUID oldJobId = existingJobId; // The ID we're updating

// Update job (should return same ID)
jobSchedulerPort.

updateJob(oldJobId, jobDefinition, jobName, parameters,
          isExternalTrigger, effectiveScheduledAt, additionalLabels);

// The updateJob method doesn't return a UUID in current implementation
// If it ever changes and returns a different UUID, we should add cleanup:
// UUID newJobId = jobSchedulerPort.updateJob(...);
// if (!oldJobId.equals(newJobId)) {
//     parameterStorageService.deleteById(oldJobId);
// }
```

**Recommendation:** This can be deferred or marked as "nice to have" since JobRunr's `createOrReplace()` respects the
provided UUID.

### Phase 4: Simplify Parameter Loading Logic

Since `jobId == parameterSetId` after this change, we can simplify the parameter loading logic for jobs with external
parameters.

**File:** `JobRunrParameterSetLoaderAdapter.java`

**Current Implementation:**

```java

@Override
public Map<String, Object> loadParameters(UUID jobId) {
    var job = storageProvider.getJobById(jobId);
    var jobDetails = job.getJobDetails();
    var jobParameters = jobDetails.getJobParameters();

    // Extract parameter set ID from job parameters
    // Then load from external storage
    ...
}
```

**Simplified Implementation:**

```java

@Override
public Map<String, Object> loadParameters(UUID jobId) {
    // Try to load from external storage first (for jobs with external params)
    // Since jobId == parameterSetId, we can try direct lookup
    try {
        return loadParametersBySetId(jobId);
    } catch (ParameterSetNotFoundException e) {
        // Not found in external storage - must be inline parameters
        // Fall back to loading from JobRunr storage
        var job = storageProvider.getJobById(jobId);
        return extractInlineParametersFromJob(job);
    }
}

private Map<String, Object> extractInlineParametersFromJob(Job job) {
    // Extract parameters from JobRunr's job storage
    // Implementation depends on JobRunr's structure
    return convertJobParametersToMap(job.getJobDetails().getJobParameters());
}
```

**Benefits:**

- ✅ Attempts direct lookup first (faster for external param jobs)
- ✅ Falls back gracefully for inline param jobs
- ✅ Simpler for the common case (external params)
- ✅ Less coupling with JobRunr's storage structure for external params

**Note:** This is an optimization that can be done after the main implementation is complete and tested. The fallback
ensures both inline and external parameter jobs work correctly.

### Phase 5: Update Job Deletion to Clean Parameters

**Already Implemented** in:

- `DeleteScheduledJobUseCase`
- `DeleteTemplateUseCase`

Both already call `parameterStoragePort.deleteById()` when deleting jobs.

**Verification Needed:** Ensure they use the job UUID (not a separate parameter set ID).

## Testing Strategy

### Unit Tests to Update

1. **ParameterStorageHelperTest**
    - Test `storeParametersForJob()` uses provided job UUID as parameter set ID
    - Test `createParameterReference()` returns correct field mapping
    - Verify parameter set ID matches job ID

2. **CreateScheduledJobUseCaseTest**
    - Test job with external parameters stores param set after job creation
    - Verify parameter set ID = job ID
    - Verify job is updated with parameter reference

3. **UpdateScheduledJobUseCaseTest**
    - Test old parameter set is deleted on update
    - Test new parameter set uses same job ID
    - Verify two-phase update pattern (delete, update, store)

4. **TemplateCloneHelperTest**
    - Test template with external parameters copies parameter set
    - Verify new parameter set has new job UUID
    - Test parameter overrides work with external storage

5. **ExecuteTemplateUseCaseTest**
    - Test executed template gets new parameter set
    - Verify parameter copying

### Integration Tests to Update

1. **Template UI Tests** (`TemplateJobUITest`, `TemplateStartUITest`, `TemplateCloneUITest`)
    - Verify templates with external parameters work end-to-end
    - Check parameter sets are created/copied correctly

2. **Database Tests**
    - Verify parameter set IDs match job IDs in database
    - Check old parameter sets are cleaned up

### Manual Testing Scenarios

1. **Create job with external parameters** → Verify param set ID = job ID in DB
2. **Update job** → Verify old param set deleted, new one has same ID
3. **Clone template** → Verify new param set created with new UUID
4. **Execute template** → Verify parameter copying works
5. **Delete job** → Verify param set is deleted
6. **Load parameters** → Verify loading by jobId works correctly (simplified loading)

## Migration Considerations

### Database Migration

**Issue:** Existing `jobrunr_control_parameter_sets` table has UUIDs that don't match job UUIDs.

**Options:**

1. **Drop and Recreate** (Development Only)
    - Simple: Delete all parameter sets, they'll be recreated on next job save

2. **Data Migration** (Production)
    - Create migration script to update parameter set IDs to match job IDs
    - Requires joining with JobRunr's job table to find matches

**Recommendation for MVP:** Accept that old jobs will have mismatched IDs until they're updated/recreated. No backward
compatibility needed - old parameter sets can be deleted and recreated.

## Error Handling Considerations

### Two-Phase Creation Failures

The two-phase approach (create job → store params → update job) introduces potential failure points:

**Scenario 1: Parameter storage fails after job creation**

- **Result:** Job exists in JobRunr with empty parameters
- **Mitigation:** Wrap in try-catch, delete job if parameter storage fails
- **Alternative:** Accept eventual consistency - parameter set can be added later when job is updated

**Scenario 2: `updateJobParameters()` fails after parameter storage**

- **Result:** Job exists, parameter set exists, but job doesn't reference it
- **Mitigation:** Parameter set is "orphaned" but has correct ID (jobId), will be replaced on next update
- **Cleanup:** Background job can clean orphaned parameter sets

**Scenario 3: Job creation fails**

- **Result:** No job, no parameters
- **Mitigation:** No special handling needed - clean failure

**Recommended Strategy:**

```java
UUID jobId = null;
try{
// Step 1: Create job
jobId =jobSchedulerPort.

scheduleJob(...);

// Step 2: Store parameters
    if(jobDefinition.

usesExternalParameters()){
        parameterStorageHelper.

storeParametersForJob(jobId, ...);

// Step 3: Update job reference
        try{
                jobSchedulerPort.

updateJobParameters(jobId, paramReference);
        }catch(
Exception e){
        LOG.

warnf(e, "Failed to update job parameters reference for job %s, "+
        "parameter set exists but not linked - will be fixed on next update",jobId);
// Don't fail - job is usable, just needs manual fix or will auto-correct on update
        }
                }

                return jobId;
}catch(
Exception e){
        // If we created a job but later steps failed, consider cleanup
        if(jobId !=null){
        LOG.

errorf(e, "Job creation partially succeeded, job %s may need cleanup",jobId);
    }
            throw new

JobSchedulingException("Failed to create job",e);
}
```

### Transaction Boundaries

**Note:** JobRunr and external parameter storage may use different databases/transactions:

- JobRunr uses its own storage (could be same DB or different)
- Parameter sets use Hibernate ORM (application database)

**Implication:** No distributed transaction, accept eventual consistency. Orphaned parameter sets will be cleaned up on
next update or by background cleanup job.

## Risks and Mitigations

| Risk                          | Impact | Mitigation                                                                     |
|-------------------------------|--------|--------------------------------------------------------------------------------|
| Job update timing issues      | MEDIUM | Store parameters after job creation, use two-phase update pattern              |
| Two-phase creation failures   | MEDIUM | Graceful error handling, accept eventual consistency, background cleanup       |
| Template cloning breaks       | HIGH   | Thorough testing of parameter copying logic                                    |
| Orphaned parameter sets       | MEDIUM | Add cleanup job to remove orphaned sets                                        |
| Database migration complexity | LOW    | Old parameter sets can be deleted and recreated on first update                |
| Performance impact            | LOW    | Two-phase create/update may add slight overhead, but improves data consistency |

## Implementation Order

1. ✅ **Phase 1.1:** Modify `ParameterStorageHelper` (add post-creation storage methods)
2. ✅ **Phase 2.2:** Modify `JobSchedulerPort` interface (add `updateJobParameters` method)
3. ✅ **Phase 1.2:** Update job creation use cases (two-phase create: job first, then params)
4. ✅ **Phase 1.3:** Update job update use cases (delete old params, update job, store new params)
5. ✅ **Phase 2.1:** Modify `TemplateCloneHelper` with parameter copying (create job first, then copy params)
6. ✅ **Phase 3:** Add cleanup logic for UUID changes (optional/defensive)
7. ✅ **Phase 4:** Simplify parameter loading logic (optimization - since jobId == parameterSetId)
8. ✅ **Phase 5:** Verify deletion cleanup
9. ✅ **Testing:** Update all tests
10. ✅ **Integration Testing:** End-to-end verification
11. ✅ **Documentation:** Update arc42 and programmer docs

## Files to Modify

### Core Implementation

1. `ParameterStorageHelper.java` - Add `storeParametersForJob()` and `createParameterReference()` methods
2. `CreateScheduledJobUseCase.java` - Two-phase create: job first, then store params with returned UUID
3. `CreateTemplateUseCase.java` - Two-phase create: job first, then store params with returned UUID
4. `UpdateScheduledJobUseCase.java` - Delete old params, update job, store new params with same UUID
5. `UpdateTemplateUseCase.java` - Delete old params, update job, store new params with same UUID
6. `TemplateCloneHelper.java` - Load source params, create job, copy params to new UUID
7. `JobSchedulerPort.java` - Add `updateJobParameters()` method
8. `JobRunrSchedulerAdapter.java` - Implement `updateJobParameters()` method
9. `JobRunrParameterSetLoaderAdapter.java` - Simplify `loadParameters()` to directly use jobId as parameterSetId (
   optimization)

### Testing

10. `ParameterStorageHelperTest.java`
11. `CreateScheduledJobUseCaseTest.java`
12. `UpdateScheduledJobUseCaseTest.java`
13. `CreateTemplateUseCaseTest.java`
14. `UpdateTemplateUseCaseTest.java`
15. `TemplateCloneHelperTest.java`
16. `JobRunrParameterSetLoaderAdapterTest.java` - Update tests for simplified loading logic
17. Integration tests (UI tests may need updates)

## Success Criteria

- ✅ Parameter set UUID always matches job UUID
- ✅ Template execution copies parameters with new UUID
- ✅ Old parameter sets are cleaned up on updates
- ✅ Parameter loading works correctly (jobId can be used directly to load parameter set)
- ✅ All tests pass
- ✅ No orphaned parameter sets in database

## Timeline Estimate

- **Phase 1:** 4-6 hours (parameter storage changes + job creation/update)
- **Phase 2:** 3-4 hours (template cloning with parameter copying + interface changes)
- **Phase 3:** 1 hour (cleanup logic - optional/defensive)
- **Phase 4:** 2-3 hours (parameter loading optimization + implementation investigation)
- **Phase 5:** 1 hour (deletion verification)
- **Testing:** 4-6 hours (unit + integration tests)
- **Documentation:** 1-2 hours

**Total:** ~16-24 hours of development work

**Note:** Time includes investigation of `updateJobParameters()` implementation details and proper merge logic.
