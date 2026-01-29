# Implementation Plan: External Parameter Storage

## 1. Overview

This document describes the implementation plan for storing job parameters in a separate repository instead of directly
in the JobRunr job table. This feature provides flexibility in parameter storage while maintaining the hexagonal
architecture principles of the JobRunr Control extension.

### Goals

1. **Configurable Storage**: Allow configuration to choose between inline parameters (current behavior) or external
   parameter storage
2. **Pluggable Repository**: Use a repository pattern that can be swapped with different implementations (Jakarta
   Persistence, other
   storage systems)
3. **Reference-based Linking**: Jobs store only a reference ID to the parameter set when using external storage
4. **Architecture Compliance**: Respect the hexagonal architecture (arc42) with proper ports and adapters
5. **Lazy Loading**: Provide an interface for loading parameter sets on-demand

## 2. Architecture Design

### 2.1 Hexagonal Architecture Alignment

Following the existing pattern from arc42.adoc, we will structure this feature across layers:

```
Domain Layer (Core):
├── ParameterSet (Entity)
├── ParameterSetId (Value Object)
├── ParameterStoragePort (Port Interface)
└── ParameterSetLoaderPort (Port Interface)

Application Layer:
├── StoreParametersUseCase
├── LoadParametersUseCase
└── DeleteParametersUseCase

Infrastructure Layer:
├── JpaParameterStorageAdapter (implements ParameterStoragePort)
└── InlineParameterStorageAdapter (implements ParameterStoragePort)

Adapter Layer:
└── Configuration updates for parameter storage strategy
```

### 2.2 Key Design Decisions

#### AD-EXT-1: Dual Storage Strategy Pattern

The system will support two parameter storage strategies:

1. **INLINE** (default, current behavior): Parameters stored directly in JobRunr's job table
2. **EXTERNAL**: Parameters stored in separate table with reference ID in job

Strategy selection via configuration property:

```properties
jobrunr.control.parameter-storage.strategy=INLINE|EXTERNAL
```

#### AD-EXT-2: Parameter Set as First-Class Domain Entity

A new `ParameterSet` entity will represent externalized parameters:

```java
public record ParameterSet(
        UUID id,
        String jobType,
        Map<String, Object> parameters,
        Instant createdAt,
        Instant lastAccessedAt
) {
}
```

#### AD-EXT-3: Reference Storage in Job Metadata

When using EXTERNAL strategy, jobs will store:

- A special parameter key: `__parameterSetId` containing the UUID reference
- Minimal metadata for traceability

#### AD-EXT-4: Port-Based Repository Pattern

Two ports define the contract for parameter storage:

1. **ParameterStoragePort**: For CRUD operations on parameter sets
2. **ParameterSetLoaderPort**: For lazy loading and retrieval by job context

## 3. Domain Layer Changes

### 3.1 New Domain Entities

#### File: `ParameterSet.java`

```java
package ch.css.jobrunr.control.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a set of job parameters stored externally.
 * Used when parameter storage strategy is EXTERNAL.
 */
public record ParameterSet(
        UUID id,
        String jobType,
        Map<String, Object> parameters,
        Instant createdAt,
        Instant lastAccessedAt
) {
    public ParameterSet {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (jobType == null || jobType.isBlank()) throw new IllegalArgumentException("jobType must not be blank");
        if (parameters == null) throw new IllegalArgumentException("parameters must not be null");
        if (createdAt == null) throw new IllegalArgumentException("createdAt must not be null");
    }

    /**
     * Creates a new parameter set with current timestamp.
     */
    public static ParameterSet create(UUID id, String jobType, Map<String, Object> parameters) {
        Instant now = Instant.now();
        return new ParameterSet(id, jobType, parameters, now, now);
    }

    /**
     * Updates last accessed timestamp.
     */
    public ParameterSet markAccessed() {
        return new ParameterSet(id, jobType, parameters, createdAt, Instant.now());
    }
}
```

#### File: `ParameterStorageStrategy.java`

```java
package ch.css.jobrunr.control.domain;

/**
 * Defines strategies for storing job parameters.
 */
public enum ParameterStorageStrategy {
    /**
     * Parameters stored inline in JobRunr job table (default).
     */
    INLINE,

    /**
     * Parameters stored in separate repository with reference ID in job.
     */
    EXTERNAL
}
```

### 3.2 New Port Interfaces

#### File: `ParameterStoragePort.java`

```java
package ch.css.jobrunr.control.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for storing and retrieving parameter sets.
 * Infrastructure adapters implement this interface to provide
 * different storage mechanisms (database, cache, etc.).
 */
public interface ParameterStoragePort {

    /**
     * Stores a parameter set.
     * @param parameterSet the parameter set to store
     */
    void store(ParameterSet parameterSet);

    /**
     * Retrieves a parameter set by ID.
     * @param id the parameter set ID
     * @return the parameter set if found
     */
    Optional<ParameterSet> findById(UUID id);

    /**
     * Deletes a parameter set by ID.
     * @param id the parameter set ID
     */
    void deleteById(UUID id);

    /**
     * Checks if external parameter storage is enabled.
     * @return true if external storage is active
     */
    boolean isExternalStorageEnabled();

    /**
     * Updates last accessed timestamp for a parameter set.
     * @param id the parameter set ID
     */
    void updateLastAccessed(UUID id);
}
```

#### File: `ParameterSetLoaderPort.java`

```java
package ch.css.jobrunr.control.domain;

import java.util.Map;
import java.util.UUID;

/**
 * Port for loading parameter sets by job context.
 * Provides lazy loading capability for jobs.
 */
public interface ParameterSetLoaderPort {

    /**
     * Loads parameters for a job by job ID.
     * Handles both inline and external parameter resolution.
     *
     * @param jobId the job ID
     * @return the job parameters
     * @throws ParameterSetNotFoundException if external reference not found
     */
    Map<String, Object> loadParameters(UUID jobId);

    /**
     * Loads parameters directly by parameter set ID.
     * Only works for jobs using external parameter storage.
     *
     * @param parameterSetId the parameter set ID
     * @return the parameters
     * @throws ParameterSetNotFoundException if not found
     */
    Map<String, Object> loadParametersBySetId(UUID parameterSetId);
}
```

### 3.3 New Domain Exception

#### File: `ParameterSetNotFoundException.java`

```java
package ch.css.jobrunr.control.domain;

import java.util.UUID;

/**
 * Thrown when a referenced parameter set cannot be found.
 */
public class ParameterSetNotFoundException extends RuntimeException {

    private final UUID parameterSetId;

    public ParameterSetNotFoundException(UUID parameterSetId) {
        super("Parameter set not found: " + parameterSetId);
        this.parameterSetId = parameterSetId;
    }

    public UUID getParameterSetId() {
        return parameterSetId;
    }
}
```

### 3.4 Update `ScheduledJobInfo` Domain Entity

Add method to check if job uses external parameters:

```java
/**
 * Checks if this job uses external parameter storage.
 * @return true if parameters are stored externally
 */
public boolean hasExternalParameters() {
    return parameters.containsKey("__parameterSetId");
}

/**
 * Gets the parameter set ID if using external storage.
 * @return the parameter set ID, or empty if inline storage
 */
public Optional<UUID> getParameterSetId() {
    Object value = parameters.get("__parameterSetId");
    if (value instanceof String str) {
        return Optional.of(UUID.fromString(str));
    }
    return Optional.empty();
}
```

## 4. Application Layer Changes

### 4.1 New Use Cases

#### File: `StoreParametersUseCase.java`

```java
package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;

/**
 * Use case for storing job parameters externally.
 */
@ApplicationScoped
public class StoreParametersUseCase {

    private static final Logger log = Logger.getLogger(StoreParametersUseCase.class);

    private final ParameterStoragePort parameterStoragePort;

    @Inject
    public StoreParametersUseCase(ParameterStoragePort parameterStoragePort) {
        this.parameterStoragePort = parameterStoragePort;
    }

    /**
     * Stores parameters externally if external storage is enabled.
     *
     * @param jobType the job type
     * @param parameters the parameters to store
     * @return the parameter set ID if stored externally, empty otherwise
     */
    public UUID execute(String jobType, Map<String, Object> parameters) {
        if (!parameterStoragePort.isExternalStorageEnabled()) {
            log.debugf("External parameter storage disabled, skipping storage for jobType: %s", jobType);
            return null;
        }

        UUID parameterSetId = UUID.randomUUID();
        ParameterSet parameterSet = ParameterSet.create(parameterSetId, jobType, parameters);

        parameterStoragePort.store(parameterSet);
        log.infof("Stored parameter set %s for jobType: %s", parameterSetId, jobType);

        return parameterSetId;
    }
}
```

#### File: `LoadParametersUseCase.java`

```java
package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.ParameterSetLoaderPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.UUID;

/**
 * Use case for loading job parameters.
 */
@ApplicationScoped
public class LoadParametersUseCase {

    private final ParameterSetLoaderPort loaderPort;

    @Inject
    public LoadParametersUseCase(ParameterSetLoaderPort loaderPort) {
        this.loaderPort = loaderPort;
    }

    public Map<String, Object> execute(UUID jobId) {
        return loaderPort.loadParameters(jobId);
    }

    public Map<String, Object> executeBySetId(UUID parameterSetId) {
        return loaderPort.loadParametersBySetId(parameterSetId);
    }
}
```

#### File: `DeleteParametersUseCase.java`

```java
package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.ParameterStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Use case for deleting externally stored parameters.
 */
@ApplicationScoped
public class DeleteParametersUseCase {

    private static final Logger log = Logger.getLogger(DeleteParametersUseCase.class);

    private final ParameterStoragePort parameterStoragePort;

    @Inject
    public DeleteParametersUseCase(ParameterStoragePort parameterStoragePort) {
        this.parameterStoragePort = parameterStoragePort;
    }

    /**
     * Deletes a parameter set if it exists.
     *
     * @param parameterSetId the parameter set ID to delete
     */
    public void execute(UUID parameterSetId) {
        if (parameterSetId == null) {
            return;
        }

        parameterStoragePort.deleteById(parameterSetId);
        log.infof("Deleted parameter set: %s", parameterSetId);
    }
}
```

### 4.2 Update Existing Use Cases

#### Update: `CreateScheduledJobUseCase.java`

Modify to use parameter storage when enabled:

```java
// After parameter validation, before scheduling
UUID parameterSetId = null;
Map<String, Object> jobParameters = validatedParameters;

if(parameterStoragePort.

isExternalStorageEnabled()){
parameterSetId =storeParametersUseCase.

execute(jobType, validatedParameters);
// Replace parameters with reference
jobParameters =Map.

of("__parameterSetId",parameterSetId.toString());
        }

// Schedule with modified parameters
UUID jobId = jobSchedulerPort.scheduleJob(
        jobDefinition,
        jobName,
        jobParameters,  // Either full params or just reference
        isExternalTrigger,
        scheduledAt,
        additionalLabels
);
```

#### Update: `DeleteScheduledJobUseCase.java`

Add cleanup of external parameters:

```java
// Before deleting job, check if it has external parameters
ScheduledJobInfo jobInfo = jobSchedulerPort.getScheduledJobById(jobId);
if(jobInfo !=null&&jobInfo.

hasExternalParameters()){
        jobInfo.

getParameterSetId().

ifPresent(deleteParametersUseCase::execute);
}

// Then delete the job
        jobSchedulerPort.

deleteScheduledJob(jobId);
```

## 5. Infrastructure Layer Changes

### 5.1 Jakarta Persistence Entity for Parameter Storage

#### File: `ParameterSetEntity.java`

```java
package ch.css.jobrunr.control.infrastructure.persistence;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for storing job parameter sets.
 * Uses standard Jakarta Persistence API.
 */
@Entity
@Table(name = "jobrunr_control_parameter_sets",
        indexes = {
                @Index(name = "idx_param_set_job_type", columnList = "job_type"),
                @Index(name = "idx_param_set_created", columnList = "created_at"),
                @Index(name = "idx_param_set_accessed", columnList = "last_accessed_at")
        })
@RegisterForReflection
public class ParameterSetEntity {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "job_type", nullable = false, length = 500)
    public String jobType;

    @Column(name = "parameters_json", nullable = false, columnDefinition = "TEXT")
    public String parametersJson;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "last_accessed_at", nullable = false)
    public Instant lastAccessedAt;

    @Version
    @Column(name = "version")
    public Long version;
}
```

### 5.2 Jakarta Persistence Adapter Implementation

#### File: `JpaParameterStorageAdapter.java`

```java
package ch.css.jobrunr.control.infrastructure.persistence;

import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-based implementation of ParameterStoragePort.
 * Stores parameters in a separate database table using Jakarta Persistence.
 */
@ApplicationScoped
@LookupIfProperty(name = "jobrunr.control.parameter-storage.strategy", stringValue = "EXTERNAL")
public class JpaParameterStorageAdapter implements ParameterStoragePort {

    private static final Logger log = Logger.getLogger(JpaParameterStorageAdapter.class);

    private final ObjectMapper objectMapper;
    private final Instance<EntityManager> entityManagerInstance;

    @Inject
    public JpaParameterStorageAdapter(ObjectMapper objectMapper, Instance<EntityManager> entityManagerInstance) {
        this.objectMapper = objectMapper;
        this.entityManagerInstance = entityManagerInstance;
    }

    private EntityManager getEntityManager() {
        if (!entityManagerInstance.isResolvable()) {
            throw new IllegalStateException(
                    "EntityManager is not available. Ensure quarkus.hibernate-orm.enabled=true " +
                            "when using EXTERNAL parameter storage strategy.");
        }
        return entityManagerInstance.get();
    }

    @Override
    @Transactional
    public void store(ParameterSet parameterSet) {
        try {
            ParameterSetEntity entity = new ParameterSetEntity();
            entity.id = parameterSet.id();
            entity.jobType = parameterSet.jobType();
            entity.parametersJson = objectMapper.writeValueAsString(parameterSet.parameters());
            entity.createdAt = parameterSet.createdAt();
            entity.lastAccessedAt = parameterSet.lastAccessedAt();

            getEntityManager().persist(entity);
            log.debugf("Stored parameter set: %s", entity.id);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize parameters", e);
        }
    }

    @Override
    public Optional<ParameterSet> findById(UUID id) {
        ParameterSetEntity entity = getEntityManager().find(ParameterSetEntity.class, id);
        if (entity == null) {
            return Optional.empty();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = objectMapper.readValue(
                    entity.parametersJson,
                    Map.class
            );

            return Optional.of(new ParameterSet(
                    entity.id,
                    entity.jobType,
                    parameters,
                    entity.createdAt,
                    entity.lastAccessedAt
            ));
        } catch (JsonProcessingException e) {
            log.errorf("Failed to deserialize parameters for set: %s", id, e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        EntityManager em = getEntityManager();
        ParameterSetEntity entity = em.find(ParameterSetEntity.class, id);
        if (entity != null) {
            em.remove(entity);
            log.debugf("Deleted parameter set: %s", id);
        }
    }

    @Override
    public boolean isExternalStorageEnabled() {
        return true;
    }

    @Override
    @Transactional
    public void updateLastAccessed(UUID id) {
        EntityManager em = getEntityManager();
        ParameterSetEntity entity = em.find(ParameterSetEntity.class, id);
        if (entity != null) {
            entity.lastAccessedAt = Instant.now();
            em.merge(entity);
        }
    }
}
```

### 5.3 Inline Adapter (Default Behavior)

#### File: `InlineParameterStorageAdapter.java`

```java
package ch.css.jobrunr.control.infrastructure.persistence;

import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

/**
 * No-op implementation for inline parameter storage (default).
 * When this adapter is active, parameters are stored directly in JobRunr's job table.
 */
@ApplicationScoped
@DefaultBean
public class InlineParameterStorageAdapter implements ParameterStoragePort {

    @Override
    public void store(ParameterSet parameterSet) {
        // No-op: parameters stored inline in job
    }

    @Override
    public Optional<ParameterSet> findById(UUID id) {
        return Optional.empty();
    }

    @Override
    public void deleteById(UUID id) {
        // No-op
    }

    @Override
    public boolean isExternalStorageEnabled() {
        return false;
    }

    @Override
    public void updateLastAccessed(UUID id) {
        // No-op
    }
}
```

### 5.4 Parameter Set Loader Implementation

#### File: `JobRunrParameterSetLoaderAdapter.java`

```java
package ch.css.jobrunr.control.infrastructure.jobrunr;

import ch.css.jobrunr.control.domain.ParameterSetLoaderPort;
import ch.css.jobrunr.control.domain.ParameterSetNotFoundException;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import ch.css.jobrunr.control.infrastructure.jobrunr.JobParameterExtractor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jobrunr.storage.StorageProvider;

import java.util.Map;
import java.util.UUID;

/**
 * Loads parameters from either JobRunr storage or external parameter repository.
 */
@ApplicationScoped
public class JobRunrParameterSetLoaderAdapter implements ParameterSetLoaderPort {

    private final StorageProvider storageProvider;
    private final ParameterStoragePort parameterStoragePort;

    @Inject
    public JobRunrParameterSetLoaderAdapter(
            StorageProvider storageProvider,
            ParameterStoragePort parameterStoragePort) {
        this.storageProvider = storageProvider;
        this.parameterStoragePort = parameterStoragePort;
    }

    @Override
    public Map<String, Object> loadParameters(UUID jobId) {
        var job = storageProvider.getJobById(jobId);
        Map<String, Object> parameters = JobParameterExtractor.extractParameters(job);

        // Check if parameters contain reference to external storage
        if (parameters.containsKey("__parameterSetId")) {
            String paramSetIdStr = (String) parameters.get("__parameterSetId");
            UUID parameterSetId = UUID.fromString(paramSetIdStr);
            return loadParametersBySetId(parameterSetId);
        }

        return parameters;
    }

    @Override
    public Map<String, Object> loadParametersBySetId(UUID parameterSetId) {
        return parameterStoragePort.findById(parameterSetId)
                .map(paramSet -> {
                    // Update last accessed timestamp
                    parameterStoragePort.updateLastAccessed(parameterSetId);
                    return paramSet.parameters();
                })
                .orElseThrow(() -> new ParameterSetNotFoundException(parameterSetId));
    }
}
```

## 6. Configuration Changes

### 6.1 Application Properties

Add new configuration options:

```properties
# Parameter storage strategy: INLINE (default) or EXTERNAL
jobrunr.control.parameter-storage.strategy=INLINE
# Optional: Enable automatic cleanup of orphaned parameter sets
jobrunr.control.parameter-storage.cleanup.enabled=true
jobrunr.control.parameter-storage.cleanup.retention-days=30
```

### 6.2 Runtime Configuration Class

#### File: `ParameterStorageConfiguration.java`

```java
package ch.css.jobrunr.control.infrastructure.config;

import ch.css.jobrunr.control.domain.ParameterStorageStrategy;
import io.smallrye.config.ConfigMapping;

import java.util.Optional;

/**
 * Configuration for parameter storage.
 */
@ConfigMapping(prefix = "jobrunr.control.parameter-storage")
public interface ParameterStorageConfiguration {

    /**
     * Strategy for storing job parameters.
     * Default: INLINE
     */
    Optional<ParameterStorageStrategy> strategy();

    /**
     * Cleanup configuration for external parameter storage.
     */
    CleanupConfig cleanup();

    interface CleanupConfig {
        /**
         * Enable automatic cleanup of orphaned parameter sets.
         * Default: true
         */
        Optional<Boolean> enabled();

        /**
         * Retention period in days for parameter sets.
         * Parameter sets older than this will be deleted if not accessed.
         * Default: 30
         */
        Optional<Integer> retentionDays();
    }
}
```

## 7. Database Migration

### 7.1 Liquibase/Flyway Migration Script

#### File: `V002__create_parameter_sets_table.sql`

```sql
-- Create table for external parameter storage
CREATE TABLE jobrunr_control_parameter_sets
(
    id               UUID         NOT NULL PRIMARY KEY,
    job_type         VARCHAR(500) NOT NULL,
    parameters_json  TEXT         NOT NULL,
    created_at       TIMESTAMP    NOT NULL,
    last_accessed_at TIMESTAMP    NOT NULL,
    version          BIGINT
);

-- Create indexes for performance
CREATE INDEX idx_param_set_job_type ON jobrunr_control_parameter_sets (job_type);
CREATE INDEX idx_param_set_created ON jobrunr_control_parameter_sets (created_at);
CREATE INDEX idx_param_set_accessed ON jobrunr_control_parameter_sets (last_accessed_at);

-- Add comments for documentation
COMMENT
ON TABLE jobrunr_control_parameter_sets IS 'Stores job parameters externally when external parameter storage is enabled';
COMMENT
ON COLUMN jobrunr_control_parameter_sets.id IS 'Unique identifier for parameter set (referenced by job)';
COMMENT
ON COLUMN jobrunr_control_parameter_sets.job_type IS 'Type of job these parameters belong to';
COMMENT
ON COLUMN jobrunr_control_parameter_sets.parameters_json IS 'JSON-serialized job parameters';
COMMENT
ON COLUMN jobrunr_control_parameter_sets.created_at IS 'Timestamp when parameter set was created';
COMMENT
ON COLUMN jobrunr_control_parameter_sets.last_accessed_at IS 'Timestamp when parameter set was last accessed';
```

## 8. UI Updates

### 8.1 Display External Parameter Reference

Update `parameter-list.html` component to show when parameters are external:

```html
{#if job.hasExternalParameters()}
<div class="alert alert-info small">
    <i class="bi bi-database"></i>
    Parameter extern gespeichert (ID: {job.getParameterSetId().orElse("unknown")})
    <button class="btn btn-sm btn-outline-primary ms-2"
            onclick="loadExternalParameters('{job.jobId}')">
        Parameter laden
    </button>
</div>
{#else}
<!-- Existing parameter display -->
<ul class="list-unstyled mb-0 small">
    {#for param in parameters}
    <li>...</li>
    {/for}
</ul>
{/if}
```

### 8.2 HTMX Endpoint for Loading External Parameters

Add endpoint to `ScheduledJobsController`:

```java

@GET
@Path("/{jobId}/external-parameters")
@RolesAllowed({"viewer", "configurator", "admin"})
@Produces(MediaType.TEXT_HTML)
public TemplateInstance loadExternalParameters(@PathParam("jobId") UUID jobId) {
    try {
        Map<String, Object> parameters = loadParametersUseCase.execute(jobId);
        return Components.parameterList(parameters, jobId.toString());
    } catch (ParameterSetNotFoundException e) {
        return Templates.error("Parameter set nicht gefunden");
    }
}
```

## 9. Testing Strategy

### 9.1 Unit Tests

1. **ParameterSet Domain Tests**
    - Test creation with validation
    - Test markAccessed behavior

2. **Use Case Tests**
    - Test StoreParametersUseCase with mock adapter
    - Test LoadParametersUseCase with both inline and external storage
    - Test DeleteParametersUseCase

3. **Adapter Tests**
    - Test JpaParameterStorageAdapter with H2 database
    - Test InlineParameterStorageAdapter (no-op behavior)
    - Test JobRunrParameterSetLoaderAdapter with mocked dependencies

### 9.2 Integration Tests

1. **End-to-End Job Creation**
    - Create job with INLINE strategy → parameters in job table
    - Create job with EXTERNAL strategy → parameters in separate table
    - Verify job execution works with both strategies

2. **Parameter Loading**
    - Load parameters from scheduled job (inline)
    - Load parameters from scheduled job (external with reference)
    - Test lazy loading behavior

3. **Migration Tests**
    - Test database migration script
    - Verify indexes are created
    - Test concurrent access to parameter sets

### 9.3 Example Test Classes

#### File: `JpaParameterStorageAdapterTest.java`

```java

@QuarkusTest
@TestTransaction
public class JpaParameterStorageAdapterTest {

    @Inject
    JpaParameterStorageAdapter adapter;

    @Test
    public void shouldStoreAndRetrieveParameterSet() {
        UUID id = UUID.randomUUID();
        Map<String, Object> params = Map.of("key", "value", "count", 42);
        ParameterSet paramSet = ParameterSet.create(id, "TestJob", params);

        adapter.store(paramSet);

        Optional<ParameterSet> retrieved = adapter.findById(id);
        assertTrue(retrieved.isPresent());
        assertEquals("TestJob", retrieved.get().jobType());
        assertEquals(42, retrieved.get().parameters().get("count"));
    }

    @Test
    public void shouldDeleteParameterSet() {
        UUID id = UUID.randomUUID();
        ParameterSet paramSet = ParameterSet.create(id, "TestJob", Map.of());

        adapter.store(paramSet);
        adapter.deleteById(id);

        Optional<ParameterSet> retrieved = adapter.findById(id);
        assertTrue(retrieved.isEmpty());
    }
}
```

## 10. Implementation Phases

### Phase 1: Core Domain & Ports (Week 1)

- [ ] Create domain entities: `ParameterSet`, `ParameterStorageStrategy`
- [ ] Create port interfaces: `ParameterStoragePort`, `ParameterSetLoaderPort`
- [ ] Create domain exception: `ParameterSetNotFoundException`
- [ ] Update `ScheduledJobInfo` with external parameter methods
- [ ] Write unit tests for domain layer

### Phase 2: Application Layer (Week 1-2)

- [ ] Implement `StoreParametersUseCase`
- [ ] Implement `LoadParametersUseCase`
- [ ] Implement `DeleteParametersUseCase`
- [ ] Update `CreateScheduledJobUseCase` to use parameter storage
- [ ] Update `DeleteScheduledJobUseCase` to cleanup parameters
- [ ] Write unit tests for use cases

### Phase 3: Infrastructure - Inline Adapter (Week 2)

- [ ] Implement `InlineParameterStorageAdapter` (default, no-op)
- [ ] Implement configuration support
- [ ] Test inline behavior (existing functionality preserved)

### Phase 4: Infrastructure - Jakarta Persistence Adapter (Week 2-3)

- [ ] Create `ParameterSetEntity` JPA entity
- [ ] Implement `JpaParameterStorageAdapter`
- [ ] Implement `JobRunrParameterSetLoaderAdapter`
- [ ] Create database migration script
- [ ] Write integration tests with H2 database

### Phase 5: Configuration & Wiring (Week 3)

- [ ] Create `ParameterStorageConfiguration` config mapping
- [ ] Implement conditional bean selection based on strategy
- [ ] Update application.properties documentation
- [ ] Test strategy switching

### Phase 6: UI Updates (Week 4)

- [ ] Update parameter display components
- [ ] Add external parameter loading endpoint
- [ ] Add visual indicators for external parameters
- [ ] Test UI with both storage strategies

### Phase 7: Testing & Documentation (Week 4-5)

- [ ] Complete integration test suite
- [ ] Performance testing with large parameter sets
- [ ] Update arc42.adoc with new architecture decisions
- [ ] Update programmers.adoc with usage examples
- [ ] Create migration guide for existing installations

### Phase 8: Optional Enhancements (Future)

- [ ] Implement automatic cleanup job for orphaned parameter sets
- [ ] Add metrics/monitoring for parameter storage
- [ ] Support for parameter versioning
- [ ] Parameter set compression for large payloads

## 11. Migration Guide for Existing Installations

### For Users Currently Running JobRunr Control

1. **No Action Required (Default Behavior)**
    - By default, `INLINE` storage is used
    - Existing installations continue to work without changes
    - Parameters remain in JobRunr job table

2. **Enabling External Storage**
    - Set `jobrunr.control.parameter-storage.strategy=EXTERNAL`
    - Run database migration to create parameter sets table
    - New jobs will use external storage
    - Existing jobs remain unchanged (mixed mode supported)

3. **Migrating Existing Jobs (Optional)**
    - Tool/script to migrate inline parameters to external storage
    - Can be done gradually (per job type or schedule)
    - Rollback capability by switching strategy back to INLINE

## 12. Alternative Storage Implementations

The port-based design allows for additional storage adapters:

### 12.1 Redis Cache Adapter

```java

@ApplicationScoped
@LookupIfProperty(name = "jobrunr.control.parameter-storage.type", stringValue = "REDIS")
public class RedisParameterStorageAdapter implements ParameterStoragePort {
    // Use Redis for fast parameter access
    // Suitable for high-volume, short-lived jobs
}
```

### 12.2 MongoDB Adapter

```java

@ApplicationScoped
@LookupIfProperty(name = "jobrunr.control.parameter-storage.type", stringValue = "MONGODB")
public class MongoParameterStorageAdapter implements ParameterStoragePort {
    // Use MongoDB for flexible schema
    // Suitable for complex, nested parameter structures
}
```

### 12.3 S3/Object Storage Adapter

```java

@ApplicationScoped
@LookupIfProperty(name = "jobrunr.control.parameter-storage.type", stringValue = "S3")
public class S3ParameterStorageAdapter implements ParameterStoragePort {
    // Use S3 for very large parameter payloads
    // Suitable for jobs with file-based parameters
}
```

## 13. Benefits & Trade-offs

### Benefits

✅ **Flexibility**: Choose storage strategy per deployment  
✅ **Scalability**: Large parameters don't bloat job table  
✅ **Modularity**: Easy to add new storage backends  
✅ **Architecture Compliance**: Follows hexagonal architecture  
✅ **Backward Compatibility**: Inline mode preserves existing behavior  
✅ **Performance**: Can optimize parameter storage independently

### Trade-offs

⚠️ **Complexity**: Additional layer of indirection for external storage  
⚠️ **Consistency**: Need to manage parameter lifecycle (cleanup)  
⚠️ **Debugging**: External references add indirection for troubleshooting  
⚠️ **Migration Overhead**: Requires database changes for external mode

## 14. Future Considerations

### Parameter Set Versioning

Track parameter changes over time for audit/compliance:

```java
public record ParameterSetVersion(
        UUID parameterSetId,
        int version,
        Map<String, Object> parameters,
        Instant createdAt,
        String createdBy
) {
}
```

### Parameter Compression

For very large parameter sets, compress JSON before storage:

```java
public interface ParameterCompressionPort {
    byte[] compress(Map<String, Object> parameters);

    Map<String, Object> decompress(byte[] compressed);
}
```

### Parameter Encryption

Sensitive parameters encrypted at rest:

```java

@ConfigMapping(prefix = "jobrunr.control.parameter-storage")
public interface ParameterStorageConfiguration {
    Optional<Boolean> encryptionEnabled();

    Optional<String> encryptionKey();
}
```

## 16. Troubleshooting

### Hibernate ORM Configuration

**Problem**: When using EXTERNAL parameter storage, you may need to properly configure Hibernate ORM.

**Solution**: Ensure the consuming application includes the Hibernate ORM dependency:

```xml

<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-orm</artifactId>
</dependency>
```

And configure Hibernate ORM appropriately in `application.properties`:

```properties
# When using INLINE strategy (default), Hibernate ORM is not needed
# When using EXTERNAL strategy, enable these:
quarkus.hibernate-orm.enabled=true
quarkus.hibernate-orm.database.generation=update
```

### Datasource Configuration Error

**Problem**: When running tests, you may encounter:

```
Datasource '<default>' is not configured.
```

**Root Cause**: Hibernate ORM Panache requires a datasource even if not actively used.

**Solution**: Either:

1. **Disable Hibernate ORM** (when using INLINE strategy):
   ```properties
   quarkus.hibernate-orm.enabled=false
   ```

2. **Configure a datasource** (when using EXTERNAL strategy):
   ```properties
   quarkus.datasource.db-kind=postgresql
   quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb
   quarkus.datasource.username=user
   quarkus.datasource.password=password
   quarkus.hibernate-orm.enabled=true
   quarkus.hibernate-orm.database.generation=update
   ```

3. **Use H2 for tests**:
   ```properties
   %test.quarkus.datasource.db-kind=h2
   %test.quarkus.datasource.jdbc.url=jdbc:h2:mem:test
   %test.quarkus.hibernate-orm.enabled=true
   %test.quarkus.hibernate-orm.database.generation=drop-and-create
   ```

### Parameter Storage Bean Not Found

**Problem**: No implementation of `ParameterStoragePort` is instantiated.

**Root Cause**: The `@LookupIfProperty` annotation only activates the bean when the property matches.

**Solution**: Verify your configuration:

```properties
# For INLINE strategy (default)
jobrunr.control.parameter-storage.strategy=INLINE
# For EXTERNAL strategy
jobrunr.control.parameter-storage.strategy=EXTERNAL
```

Ensure only one implementation is active at a time based on the strategy.

## 17. References

- **arc42.adoc**: Hexagonal architecture design principles
- **Quarkus Panache
  **: [https://quarkus.io/guides/hibernate-orm-panache](https://quarkus.io/guides/hibernate-orm-panache)
- **JobRunr Pro**: v8.4.1 API documentation
- **Jackson ObjectMapper**: JSON serialization for parameters

---

**Document Version**: 1.0  
**Last Updated**: 2026-01-28  
**Author**: JobRunr Control Development Team
