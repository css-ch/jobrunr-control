# Test Coverage Improvement Plan

> **Focus**: Quality over Quantity - Testing critical paths, edge cases, and business logic

---

## Executive Summary

**Current State**:

- **13 test files** for **123 production files** (~10% file coverage)
- Heavy focus on UI/E2E tests (5 Playwright tests)
- Architecture tests (3 ArchUnit tests)
- Minimal unit and integration tests
- **Critical Gap**: No tests for use cases, adapters, or business logic

**Goals**:

- **Quality First**: Test critical business logic, not trivial getters/setters
- **Risk-Based**: Prioritize high-risk, high-value components
- **Maintainable**: Write clear, focused tests that serve as documentation
- **Fast Feedback**: Unit tests run in milliseconds, integration tests in seconds

**Target Coverage** (By Priority):

1. **Application Layer (Use Cases)**: 80%+ coverage
2. **Domain Layer**: 90%+ coverage
3. **Infrastructure Adapters**: 70%+ coverage
4. **Controllers**: 60%+ (integration tests)

---

## 🎯 Phase 1: Critical Business Logic (Weeks 1-2)

### Priority: CRITICAL

These components handle core business logic and must be thoroughly tested.

### 1.1 Scheduling Use Cases

#### `CreateScheduledJobUseCaseTest.java`

**Why Critical**: Core feature - creates scheduled jobs with parameter validation
**Test Focus**:

- ✅ Happy path: Create job with valid parameters
- ✅ External parameters: Store in parameter set
- ✅ Inline parameters: Include in job request
- ✅ Validation: Reject invalid job types
- ✅ Edge case: Empty parameters
- ✅ Edge case: Duplicate job names
- ✅ Integration: Verify scheduler adapter is called correctly

**Estimated Tests**: 8-10 test methods

```java

@Test
void shouldCreateScheduledJobWithInlineParameters() {
    // Arrange
    String jobType = "TestJob";
    String jobName = "My Test Job";
    Map<String, String> params = Map.of("param1", "value1");
    Instant scheduledAt = Instant.now().plus(Duration.ofHours(1));

    when(jobDefinitionPort.findByType(jobType)).thenReturn(Optional.of(testJobDef));
    when(jobSchedulerPort.scheduleJob(any(), any(), any(), anyBoolean(), any()))
            .thenReturn(UUID.randomUUID());

    // Act
    UUID jobId = useCase.execute(jobType, jobName, params, scheduledAt, false);

    // Assert
    assertNotNull(jobId);
    verify(jobSchedulerPort).scheduleJob(
            eq(testJobDef), eq(jobName), eq(params), eq(false), eq(scheduledAt)
    );
    verify(parameterStoragePort, never()).store(any());
}

@Test
void shouldCreateScheduledJobWithExternalParameters() {
    // Test external parameter storage logic
}

@Test
void shouldValidateJobTypeExists() {
    // Test validation fails for unknown job type
}
```

---

#### `UpdateScheduledJobUseCaseTest.java`

**Why Critical**: Updates can corrupt job state if not handled correctly
**Test Focus**:

- ✅ Update job with changed parameters
- ✅ Switch between inline/external parameter storage
- ✅ Clean up old parameter sets
- ✅ Handle concurrent updates
- ✅ Validation: Job must exist

**Estimated Tests**: 7-9 test methods

---

#### `DeleteScheduledJobUseCaseTest.java`

**Why Critical**: Must clean up parameter sets to avoid orphaned data
**Test Focus**:

- ✅ Delete job and associated parameter set
- ✅ Delete job without parameter set
- ✅ Handle missing job gracefully
- ✅ Verify cleanup filter is not called for immediate deletion

**Estimated Tests**: 4-5 test methods

---

#### `ExecuteScheduledJobUseCaseTest.java`

**Why Critical**: Triggers job execution with parameter overrides
**Test Focus**:

- ✅ Execute with parameter overrides
- ✅ Execute without overrides
- ✅ Merge overrides with existing parameters
- ✅ Handle external parameters correctly
- ✅ Validation: Job must exist

**Estimated Tests**: 5-6 test methods

---

### 1.2 Template Use Cases

#### `CreateTemplateUseCaseTest.java`

**Test Focus**:

- ✅ Create template with "template" label
- ✅ Store parameters correctly
- ✅ Scheduled far in future (2999-12-31)
- ✅ Validation

**Estimated Tests**: 4-5 test methods

---

#### `CloneTemplateUseCaseTest.java`

**Why Critical**: Complex logic for cloning with date suffixes
**Test Focus**:

- ✅ Clone template with auto-generated suffix
- ✅ Clone with custom suffix
- ✅ Clone with parameter overrides
- ✅ Preserve job type and structure
- ✅ Don't copy "template" label to clone

**Estimated Tests**: 6-7 test methods

---

#### `ExecuteTemplateUseCaseTest.java`

**Why Critical**: Combines cloning + execution with overrides
**Test Focus**:

- ✅ Clone and execute immediately
- ✅ Apply parameter overrides
- ✅ Generate unique job name
- ✅ Validation

**Estimated Tests**: 5-6 test methods

---

### 1.3 Parameter Handling

#### `ResolveParametersUseCaseTest.java`

**Why Critical**: Core logic for resolving external parameter sets
**Test Focus**:

- ✅ Resolve inline parameters (pass through)
- ✅ Resolve external parameters (load from storage)
- ✅ Detect external storage usage
- ✅ Handle missing parameter sets gracefully
- ✅ Parse JSON correctly

**Estimated Tests**: 6-7 test methods

```java

@Test
void shouldResolveInlineParameters() {
    // Arrange
    Map<String, Object> inlineParams = Map.of("key", "value");

    // Act
    Map<String, Object> resolved = useCase.execute(inlineParams);

    // Assert
    assertEquals(inlineParams, resolved);
    verify(parameterStoragePort, never()).load(any());
}

@Test
void shouldResolveExternalParameters() {
    // Arrange
    String paramSetId = "ps-123";
    Map<String, Object> params = Map.of("parameterSetId", paramSetId);
    String storedJson = "{\"key\":\"value\"}";

    when(parameterStoragePort.load(paramSetId)).thenReturn(Optional.of(storedJson));

    // Act
    Map<String, Object> resolved = useCase.execute(params);

    // Assert
    assertEquals("value", resolved.get("key"));
    verify(parameterStoragePort).load(paramSetId);
}
```

---

#### `StoreParametersUseCaseTest.java`

**Test Focus**:

- ✅ Generate unique parameter set ID
- ✅ Serialize parameters to JSON
- ✅ Store in parameter storage
- ✅ Return modified parameter map with ID reference

**Estimated Tests**: 4-5 test methods

---

#### `LoadParametersUseCaseTest.java`

**Test Focus**:

- ✅ Load and deserialize parameters
- ✅ Handle missing parameter set
- ✅ Handle corrupted JSON

**Estimated Tests**: 3-4 test methods

---

#### `DeleteParametersUseCaseTest.java`

**Test Focus**:

- ✅ Delete parameter set by ID
- ✅ Extract ID from parameter map
- ✅ Handle missing ID gracefully

**Estimated Tests**: 3-4 test methods

---

### 1.4 Validation

#### `JobParameterValidatorTest.java`

**Why Critical**: Prevents invalid data from entering the system
**Test Focus**:

- ✅ Validate required parameters present
- ✅ Validate parameter types (String, Integer, Boolean, Date, DateTime, Enum)
- ✅ Validate enum values are valid
- ✅ Validate multi-enum values
- ✅ Validate default values
- ✅ Provide clear error messages

**Estimated Tests**: 10-12 test methods

```java

@Test
void shouldValidateRequiredParameterPresent() {
    // Arrange
    JobParameter param = new JobParameter("name", JobParameterType.STRING, true, null, List.of());
    Map<String, String> values = Map.of("name", "John");

    // Act & Assert
    assertDoesNotThrow(() -> validator.validate(List.of(param), values));
}

@Test
void shouldRejectMissingRequiredParameter() {
    // Arrange
    JobParameter param = new JobParameter("name", JobParameterType.STRING, true, null, List.of());
    Map<String, String> values = Map.of();

    // Act & Assert
    ValidationException ex = assertThrows(
            ValidationException.class,
            () -> validator.validate(List.of(param), values)
    );
    assertTrue(ex.getMessage().contains("name"));
    assertTrue(ex.getMessage().contains("required"));
}

@Test
void shouldValidateIntegerType() {
    // Test integer parsing
}

@Test
void shouldValidateEnumValues() {
    // Test enum validation against allowed values
}
```

---

## 🎯 Phase 2: Infrastructure Adapters (Weeks 3-4)

### Priority: HIGH

Adapters connect to external systems and must handle errors gracefully.

### 2.1 JobRunr Scheduler Adapter

#### `JobRunrSchedulerAdapterTest.java`

**Why Important**: Interfaces with JobRunr Pro - must handle edge cases
**Test Focus**:

- ✅ Schedule job successfully
- ✅ Update scheduled job
- ✅ Delete scheduled job
- ✅ Get scheduled jobs
- ✅ Handle JobRunr exceptions
- ✅ Map domain models to JobRunr models correctly

**Estimated Tests**: 8-10 test methods
**Type**: Integration test (use JobRunr test utilities)

---

#### `JobInvokerTest.java`

**Test Focus**:

- ✅ Invoke job with parameters
- ✅ Handle inline vs external parameters
- ✅ Create proper JobRequest instances
- ✅ Handle missing job definitions

**Estimated Tests**: 5-6 test methods

---

### 2.2 Parameter Storage

#### `JpaParameterStorageAdapterTest.java`

**Why Important**: Database operations must be transactional and correct
**Test Focus**:

- ✅ Store parameter set
- ✅ Load parameter set by ID
- ✅ Delete parameter set
- ✅ Handle transactions correctly
- ✅ Handle unique constraint violations

**Estimated Tests**: 6-7 test methods
**Type**: Integration test with H2 database

```java

@QuarkusTest
class JpaParameterStorageAdapterTest {

    @Inject
    JpaParameterStorageAdapter adapter;

    @Inject
    TransactionManager tm;

    @Test
    @Transactional
    void shouldStoreAndLoadParameterSet() throws Exception {
        // Arrange
        String id = "test-id-" + UUID.randomUUID();
        String json = "{\"key\":\"value\"}";

        // Act
        tm.begin();
        adapter.store(id, json);
        tm.commit();

        // Assert
        Optional<String> loaded = adapter.load(id);
        assertTrue(loaded.isPresent());
        assertEquals(json, loaded.get());
    }

    @Test
    @Transactional
    void shouldHandleMissingParameterSet() {
        // Act
        Optional<String> result = adapter.load("non-existent");

        // Assert
        assertFalse(result.isPresent());
    }
}
```

---

#### `InlineParameterStorageAdapterTest.java`

**Test Focus**:

- ✅ Store returns empty (no-op)
- ✅ Load returns empty (no-op)
- ✅ Delete is no-op

**Estimated Tests**: 3 test methods

---

### 2.3 JobRunr Execution Adapter

#### `JobRunrExecutionAdapterTest.java`

**Test Focus**:

- ✅ Get execution history
- ✅ Get batch progress
- ✅ Map JobRunr job states to domain states
- ✅ Handle missing jobs

**Estimated Tests**: 6-7 test methods

---

### 2.4 Filters

#### `ParameterCleanupJobFilterTest.java`

**Why Important**: Must clean up orphaned parameter sets
**Test Focus**:

- ✅ Delete parameter set on job success with delete policy
- ✅ Delete parameter set on job failure with delete policy
- ✅ Preserve parameter set when policy is false
- ✅ Handle missing parameter sets gracefully
- ✅ Only process jobs with external parameters

**Estimated Tests**: 7-8 test methods

```java

@Test
void shouldDeleteParameterSetOnJobSuccessWhenConfigured() {
    // Arrange
    Job job = createJobWithExternalParams("ps-123");
    JobSettings settings = new JobSettings(
            "Test", false, 3, List.of(), List.of(),
            "", "", "", "", "", "true", "" // deleteOnSuccess=true
    );
    when(jobDefinitionPort.findByType(any())).thenReturn(Optional.of(
            createJobDefWithSettings(settings)
    ));

    // Act
    filter.onStateElection(job, new SucceededState(job));

    // Assert
    verify(parameterStoragePort).delete("ps-123");
}

@Test
void shouldNotDeleteParameterSetWhenNotConfigured() {
    // Test preservation logic
}
```

---

## 🎯 Phase 3: Domain Layer (Week 5)

### Priority: MEDIUM-HIGH

Domain objects contain business rules that must be tested.

### 3.1 Domain Model Tests

#### `JobDefinitionTest.java`

**Test Focus**:

- ✅ Create with valid data
- ✅ Validate required fields
- ✅ JobSettings validation
- ✅ Parameter validation

**Estimated Tests**: 5-6 test methods

---

#### `JobParameterTest.java`

**Test Focus**:

- ✅ Create different parameter types
- ✅ Validate enum values
- ✅ Default value logic
- ✅ Required vs optional

**Estimated Tests**: 6-7 test methods

---

#### `JobExecutionInfoTest.java`

**Test Focus**:

- ✅ Create from job data
- ✅ Status transitions
- ✅ Duration calculations
- ✅ Metadata handling

**Estimated Tests**: 5-6 test methods

---

#### `BatchProgressTest.java`

**Test Focus**:

- ✅ Calculate progress percentage
- ✅ Handle zero total
- ✅ Progress states

**Estimated Tests**: 4-5 test methods

---

## 🎯 Phase 4: Controller Integration Tests (Week 6)

### Priority: MEDIUM

Verify HTTP/HTMX behavior and response formatting.

### 4.1 Controller Tests

#### `ScheduledJobsControllerTest.java`

**Test Focus**:

- ✅ GET /table returns correct HTMX response
- ✅ POST creates job and returns modal close trigger
- ✅ PUT updates job correctly
- ✅ DELETE removes job
- ✅ Parameter loading via HTMX
- ✅ Error handling returns proper HTMX error response
- ✅ Pagination works correctly
- ✅ Search and filtering

**Estimated Tests**: 12-15 test methods
**Type**: Integration test with RestAssured

```java

@QuarkusTest
@TestHTTPEndpoint(ScheduledJobsController.class)
class ScheduledJobsControllerTest {

    @Test
    void shouldReturnScheduledJobsTable() {
        given()
                .when()
                .get("/table")
                .then()
                .statusCode(200)
                .contentType("text/html");
    }

    @Test
    void shouldCreateJobAndReturnModalClose() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("jobType", "TestJob")
                .formParam("jobName", "Test")
                .formParam("triggerType", "scheduled")
                .formParam("scheduledAt", "2026-02-01T10:00:00")
                .when()
                .post()
                .then()
                .statusCode(200)
                .header("HX-Trigger", "closeModal");
    }

    @Test
    void shouldReturnErrorOnInvalidJobType() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("jobType", "NonExistent")
                .when()
                .post()
                .then()
                .statusCode(200)
                .header("HX-Trigger", "scrollToError")
                .body(containsString("alert-danger"));
    }
}
```

---

#### `TemplatesControllerTest.java`

**Test Focus**:

- Similar to ScheduledJobsController but for templates
- Template-specific: clone functionality

**Estimated Tests**: 10-12 test methods

---

#### `JobExecutionsControllerTest.java`

**Test Focus**:

- GET execution history
- Filter by status
- Batch progress retrieval

**Estimated Tests**: 6-8 test methods

---

## 🎯 Phase 5: Helper & Utility Tests (Week 7)

### Priority: LOW-MEDIUM

### 5.1 Utility Classes

#### `PaginationHelperTest.java`

**Test Focus**:

- ✅ Paginate with various page sizes
- ✅ Handle empty lists
- ✅ Edge cases: page out of bounds
- ✅ Calculate page ranges correctly

**Estimated Tests**: 6-7 test methods

---

#### `JobSearchUtilsTest.java`

**Test Focus**:

- ✅ Search scheduled jobs
- ✅ Search executions
- ✅ Case-insensitive search
- ✅ Search multiple fields

**Estimated Tests**: 5-6 test methods

---

#### `ParameterStorageHelperTest.java`

**Test Focus**:

- ✅ Generate unique IDs
- ✅ Extract parameter set ID
- ✅ Check if uses external storage

**Estimated Tests**: 4-5 test methods

---

## 📊 Test Coverage Targets

### By Layer

| Layer                   | Current      | Target  | Priority |
|-------------------------|--------------|---------|----------|
| Application (Use Cases) | 0%           | 80%     | CRITICAL |
| Domain                  | 20%          | 90%     | HIGH     |
| Infrastructure Adapters | 0%           | 70%     | HIGH     |
| Controllers             | 0% (UI only) | 60%     | MEDIUM   |
| Utilities               | 0%           | 80%     | LOW      |
| **Overall**             | **~10%**     | **75%** | -        |

### By Test Type

| Test Type          | Current Count | Target Count | Purpose                       |
|--------------------|---------------|--------------|-------------------------------|
| Unit Tests         | ~5            | ~120         | Fast feedback, business logic |
| Integration Tests  | ~2            | ~30          | Component interactions        |
| UI Tests (E2E)     | 5             | 5-7          | Critical user journeys        |
| Architecture Tests | 3             | 3-5          | Enforce design rules          |
| **Total**          | **~15**       | **~160**     | -                             |

---

## 🛠 Testing Infrastructure

### Tools & Frameworks

1. **JUnit 5**: Test framework
2. **Mockito**: Mocking dependencies
3. **AssertJ**: Fluent assertions
4. **Quarkus Test**: Integration testing
5. **RestAssured**: HTTP/REST testing
6. **H2 Database**: In-memory database for integration tests
7. **ArchUnit**: Architecture validation (existing)
8. **Playwright**: UI testing (existing)

### Test Conventions

```java
// Naming convention: MethodName_StateUnderTest_ExpectedBehavior
@Test
void execute_ValidParameters_CreatesScheduledJob() {
    // Arrange
    // ... setup

    // Act
    // ... execute

    // Assert
    // ... verify
}

// Use descriptive test data builders
JobDefinition createTestJobDefinition() {
    return new JobDefinition(
            "TestJob",
            false,
            "com.example.TestJobRequest",
            "com.example.TestJobHandler",
            List.of(),
            true,
            createDefaultJobSettings(),
            false,
            null
    );
}
```

---

## 📈 Success Metrics

### Code Quality Metrics

1. **Coverage**: 75%+ overall, 80%+ for use cases
2. **Test Speed**: Unit tests < 5 seconds total
3. **Test Reliability**: 0 flaky tests
4. **Mutation Coverage**: 70%+ (use PIT mutation testing)

### Process Metrics

1. **Test First**: Write tests before fixing bugs
2. **TDD**: Use TDD for new features (Red-Green-Refactor)
3. **CI/CD**: All tests run on every commit
4. **Documentation**: Tests serve as living documentation

---

## 🚀 Implementation Approach

### Week-by-Week Plan

**Week 1**: Phase 1.1 - Scheduling Use Cases (40-45 tests)
**Week 2**: Phase 1.2-1.4 - Templates, Parameters, Validation (35-40 tests)
**Week 3**: Phase 2.1-2.2 - Scheduler & Storage Adapters (20-25 tests)
**Week 4**: Phase 2.3-2.4 - Execution & Filters (15-20 tests)
**Week 5**: Phase 3 - Domain Layer (20-25 tests)
**Week 6**: Phase 4 - Controller Integration Tests (30-35 tests)
**Week 7**: Phase 5 - Utilities & Cleanup (15-20 tests)

### Daily Workflow

1. **Morning**: Pick highest priority untested component
2. **Write Tests**: Start with happy path, add edge cases
3. **Run Tests**: Ensure they pass and are fast
4. **Review**: Self-review for clarity and value
5. **Commit**: Small, focused commits with good messages

### Code Review Checklist

- [ ] Tests have clear, descriptive names
- [ ] Each test verifies one thing
- [ ] Arrange-Act-Assert structure is followed
- [ ] No duplicate test setup (use helpers)
- [ ] Tests are independent (no shared state)
- [ ] Mocks are used appropriately (only for boundaries)
- [ ] Integration tests test real integrations
- [ ] Tests run fast (unit tests < 100ms each)
- [ ] Tests provide value (not testing trivial code)

---

## 🎓 Testing Best Practices

### What to Test

✅ **DO Test**:

- Business logic and rules
- Edge cases and boundary conditions
- Error handling and validation
- Integration points with external systems
- Complex algorithms and calculations
- Security and authorization logic

❌ **DON'T Test**:

- Trivial getters/setters without logic
- Framework code (e.g., Quarkus internals)
- Third-party libraries (trust their tests)
- Configuration files
- Generated code

### Test Quality Guidelines

1. **F.I.R.S.T. Principles**:
    - **F**ast: Tests should run in milliseconds
    - **I**ndependent: Tests don't depend on each other
    - **R**epeatable: Same result every time
    - **S**elf-validating: Pass/fail, no manual inspection
    - **T**imely: Written before/with production code

2. **Arrange-Act-Assert**:
   ```java
   @Test
   void shouldDoSomething() {
       // Arrange: Set up test data and mocks
       var input = createTestInput();
       when(dependency.method()).thenReturn(expected);
       
       // Act: Execute the code under test
       var result = systemUnderTest.execute(input);
       
       // Assert: Verify the result
       assertThat(result).isEqualTo(expected);
       verify(dependency).method();
   }
   ```

3. **Test Data Builders**:
   ```java
   class JobDefinitionBuilder {
       private String jobType = "DefaultJob";
       private boolean isBatch = false;
       
       JobDefinitionBuilder withJobType(String jobType) {
           this.jobType = jobType;
           return this;
       }
       
       JobDefinition build() {
           return new JobDefinition(jobType, isBatch, ...);
       }
   }
   
   // Usage:
   JobDefinition job = new JobDefinitionBuilder()
       .withJobType("CustomJob")
       .build();
   ```

---

## 📝 Appendix

### A. Test Template

```java
package ch.css.jobrunr.control.application.scheduling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateScheduledJobUseCase")
class CreateScheduledJobUseCaseTest {

    @Mock
    private JobDefinitionPort jobDefinitionPort;

    @Mock
    private JobSchedulerPort jobSchedulerPort;

    @Mock
    private ParameterStoragePort parameterStoragePort;

    @InjectMocks
    private CreateScheduledJobUseCase useCase;

    private JobDefinition testJobDef;

    @BeforeEach
    void setUp() {
        testJobDef = createTestJobDefinition();
    }

    @Test
    @DisplayName("should create scheduled job with valid inline parameters")
    void execute_ValidInlineParameters_CreatesScheduledJob() {
        // Arrange
        String jobType = "TestJob";
        String jobName = "My Test Job";
        Map<String, String> params = Map.of("param1", "value1");

        when(jobDefinitionPort.findByType(jobType))
                .thenReturn(Optional.of(testJobDef));
        when(jobSchedulerPort.scheduleJob(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(UUID.randomUUID());

        // Act
        UUID result = useCase.execute(jobType, jobName, params, null, false);

        // Assert
        assertThat(result).isNotNull();
        verify(jobSchedulerPort).scheduleJob(
                eq(testJobDef),
                eq(jobName),
                eq(params),
                eq(false),
                any()
        );
        verify(parameterStoragePort, never()).store(any(), any());
    }

    // Helper methods
    private JobDefinition createTestJobDefinition() {
        return new JobDefinition(
                "TestJob",
                false,
                "com.example.TestJobRequest",
                "com.example.TestJobHandler",
                List.of(),
                true,
                createDefaultJobSettings(),
                false,
                null
        );
    }

    private JobSettings createDefaultJobSettings() {
        return new JobSettings("", false, 3, List.of(), List.of(),
                "", "", "", "", "", "", "");
    }
}
```

### B. Integration Test Template

```java
package ch.css.jobrunr.control.adapter.ui;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@DisplayName("ScheduledJobsController Integration Tests")
class ScheduledJobsControllerTest {

    @Test
    @DisplayName("should return scheduled jobs table")
    void getScheduledJobsTable_DefaultParameters_ReturnsHTMLTable() {
        given()
                .when()
                .get("/q/jobrunr-control/scheduled/table")
                .then()
                .statusCode(200)
                .contentType("text/html")
                .body(containsString("scheduled-jobs-table"));
    }

    @Test
    @DisplayName("should create job and close modal")
    void createJob_ValidParameters_ReturnsModalCloseResponse() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("jobType", "ExampleBatchJob")
                .formParam("jobName", "Test Batch Job")
                .formParam("triggerType", "external")
                .when()
                .post("/q/jobrunr-control/scheduled")
                .then()
                .statusCode(200)
                .header("HX-Trigger", "closeModal")
                .contentType("text/html");
    }
}
```

---

## 🎯 Conclusion

This plan focuses on **quality over quantity**:

1. **Risk-Based**: Test critical business logic first
2. **Value-Driven**: Each test provides real value
3. **Maintainable**: Clear, focused tests that serve as documentation
4. **Fast**: Quick feedback loop for developers

**Expected Outcome**:

- 75% overall test coverage (from ~10%)
- 160 high-quality tests (from 15)
- Critical business logic 80%+ covered
- Fast, reliable test suite
- Tests as living documentation

**Timeline**: 7 weeks for full implementation
**Effort**: ~2-3 hours per day

---

*This plan prioritizes testing the most critical and complex parts of the application first, ensuring that the core
business logic is well-protected before moving to lower-priority components.*
