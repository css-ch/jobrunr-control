# Testing Guidelines

This directory contains tests for the JobRunr Control extension.

## 📁 Test Structure

```
test/
├── java/
│   └── ch/css/jobrunr/control/
│       ├── application/           # Use case unit tests
│       │   ├── scheduling/        # Job scheduling logic
│       │   ├── template/          # Template management
│       │   ├── monitoring/        # Job monitoring
│       │   ├── parameters/        # Parameter handling
│       │   └── discovery/         # Job discovery
│       ├── domain/                # Domain model tests
│       ├── infrastructure/        # Adapter integration tests
│       │   ├── jobrunr/          # JobRunr adapters
│       │   └── persistence/       # JPA adapters
│       ├── adapter/               # Controller integration tests
│       │   ├── ui/               # UI controllers
│       │   └── rest/             # REST resources
│       └── architecture/          # ArchUnit tests
└── resources/
    └── application.properties     # Test configuration
```

## 🎯 Test Coverage Goals

| Layer                   | Target Coverage | Priority |
|-------------------------|-----------------|----------|
| Application (Use Cases) | 80%+            | CRITICAL |
| Domain                  | 90%+            | HIGH     |
| Infrastructure          | 70%+            | HIGH     |
| Controllers             | 60%+            | MEDIUM   |

## 📚 Test Types

### 1. Unit Tests

**Location**: `application/`, `domain/`
**Purpose**: Test business logic in isolation
**Speed**: < 100ms per test
**Dependencies**: Mocked

```java

@ExtendWith(MockitoExtension.class)
class CreateScheduledJobUseCaseTest {
    @Mock
    private JobSchedulerPort jobSchedulerPort;

    @InjectMocks
    private CreateScheduledJobUseCase useCase;

    @Test
    void shouldCreateJob() {
        // Test implementation
    }
}
```

### 2. Integration Tests

**Location**: `infrastructure/`, `adapter/`
**Purpose**: Test component interactions
**Speed**: < 1 second per test
**Dependencies**: Real (in-memory DB, test containers)

```java

@QuarkusTest
class JpaParameterStorageAdapterTest {
    @Inject
    JpaParameterStorageAdapter adapter;

    @Test
    @Transactional
    void shouldStoreAndLoadParameters() {
        // Test implementation
    }
}
```

### 3. UI Tests

**Location**: `../example/src/test/`
**Purpose**: Test critical user journeys
**Speed**: < 5 seconds per test
**Dependencies**: Full application with Playwright

```java

@QuarkusTest
class JobSchedulingUITest {
    @Test
    void shouldScheduleJobViaUI() {
        // Playwright test
    }
}
```

### 4. Architecture Tests

**Location**: `architecture/`
**Purpose**: Enforce design rules
**Speed**: < 1 second per test
**Dependencies**: ArchUnit

```java

@Test
void useCasesShouldOnlyDependOnPorts() {
    classes()
            .that().resideInAPackage("..application..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..application..", "..domain..", "java..")
            .check(importedClasses);
}
```

## 🧪 Test Naming Conventions

### Test Class Names

- **Pattern**: `{ClassName}Test`
- **Examples**: `CreateScheduledJobUseCaseTest`, `JobDefinitionTest`

### Test Method Names

- **Pattern**: `methodName_StateUnderTest_ExpectedBehavior`
- **Alternative**: Use `@DisplayName` for readable descriptions

```java
// Approach 1: Method name
@Test
void execute_ValidParameters_CreatesScheduledJob() {
}

// Approach 2: DisplayName (preferred)
@Test
@DisplayName("should create scheduled job with valid parameters")
void shouldCreateScheduledJobWithValidParameters() {
}
```

## 🏗️ Test Structure (Arrange-Act-Assert)

```java

@Test
void shouldDoSomething() {
    // Arrange: Set up test data and mocks
    var input = createTestInput();
    when(dependency.method()).thenReturn(expectedValue);

    // Act: Execute the code under test
    var result = systemUnderTest.execute(input);

    // Assert: Verify the result
    assertThat(result)
            .isNotNull()
            .isEqualTo(expectedValue);
    verify(dependency).method();
}
```

## 📋 Test Organization

Use nested test classes to group related tests:

```java

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateScheduledJobUseCase")
class CreateScheduledJobUseCaseTest {

    @Nested
    @DisplayName("When creating job with inline parameters")
    class InlineParametersTests {
        @Test
        @DisplayName("should create job successfully")
        void shouldCreateJob() {
        }
    }

    @Nested
    @DisplayName("When creating job with external parameters")
    class ExternalParametersTests {
        @Test
        @DisplayName("should store parameters")
        void shouldStoreParameters() {
        }
    }

    @Nested
    @DisplayName("When validation fails")
    class ValidationTests {
        @Test
        @DisplayName("should throw exception for invalid input")
        void shouldThrowException() {
        }
    }
}
```

## 🛠️ Testing Tools

- **JUnit 5**: Test framework
- **Mockito**: Mocking framework
- **AssertJ**: Fluent assertions (preferred over JUnit assertions)
- **Quarkus Test**: Integration testing
- **RestAssured**: HTTP testing
- **ArchUnit**: Architecture testing
- **Playwright**: UI testing

## ✅ Best Practices

### DO ✅

1. **Write clear test names** that describe what is being tested
2. **Use AssertJ** for readable assertions
3. **Test one thing per test** - focused tests are easier to debug
4. **Use test data builders** for maintainable test data
5. **Mock only external dependencies** - test real collaborators when possible
6. **Verify behavior, not implementation** - test what, not how
7. **Write tests for edge cases** - null, empty, invalid data
8. **Keep tests independent** - no shared state between tests

```java
// Good: Clear, focused test
@Test
void shouldRejectEmptyJobName() {
    assertThatThrownBy(() -> useCase.execute("Job", "", Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Job name");
}

// Good: AssertJ fluent assertions
assertThat(result)
    .

isNotNull()
    .

extracting(Job::getName)
    .

isEqualTo("Test Job");
```

### DON'T ❌

1. **Don't test trivial code** - getters/setters without logic
2. **Don't test framework code** - trust Quarkus/JPA/etc.
3. **Don't write flaky tests** - tests must be deterministic
4. **Don't share state** between tests - use `@BeforeEach` not class fields
5. **Don't test implementation details** - test public API
6. **Don't ignore test failures** - fix immediately
7. **Don't use Thread.sleep()** - use proper synchronization

```java
// Bad: Testing trivial getter
@Test
void getNameShouldReturnName() {
    job.setName("Test");
    assertEquals("Test", job.getName()); // No value
}

// Bad: Flaky test
@Test
void shouldCompleteAsynchronously() throws InterruptedException {
    service.executeAsync();
    Thread.sleep(1000); // Unreliable!
    assertTrue(service.isComplete());
}
```

## 🔍 Test Data Builders

Create reusable builders for complex test data:

```java
class JobDefinitionBuilder {
    private String jobType = "DefaultJob";
    private boolean isBatch = false;
    private boolean usesExternalParams = false;

    public JobDefinitionBuilder withJobType(String jobType) {
        this.jobType = jobType;
        return this;
    }

    public JobDefinitionBuilder asBatchJob() {
        this.isBatch = true;
        return this;
    }

    public JobDefinitionBuilder withExternalParameters() {
        this.usesExternalParams = true;
        return this;
    }

    public JobDefinition build() {
        return new JobDefinition(
                jobType,
                isBatch,
                // ... other fields
                usesExternalParams,
                usesExternalParams ? "parameterSetId" : null
        );
    }
}

// Usage in tests:
JobDefinition job = new JobDefinitionBuilder()
        .withJobType("BatchJob")
        .asBatchJob()
        .withExternalParameters()
        .build();
```

## 🚀 Running Tests

### Run all tests

```bash
./mvnw test
```

### Run specific test class

```bash
./mvnw test -Dtest=CreateScheduledJobUseCaseTest
```

### Run tests with coverage

```bash
./mvnw verify jacoco:report
```

### Run only unit tests (fast)

```bash
./mvnw test -Dgroups=unit
```

### Run only integration tests

```bash
./mvnw test -Dgroups=integration
```

## 📊 Coverage Reports

Coverage reports are generated in:

```
target/site/jacoco/index.html
```

Open in browser to see:

- Overall coverage percentage
- Coverage by package
- Coverage by class
- Line-by-line coverage

## 🎓 Example Tests

See comprehensive examples:

- **Unit Test**: `CreateScheduledJobUseCaseTest.java` - Shows nested tests, mocking, validation
- **Integration Test**: Template in `TEST_COVERAGE_PLAN.md` Appendix B
- **Test Data Builders**: See above section

## 📚 Resources

- **Test Coverage Plan**: [TEST_COVERAGE_PLAN.md](../../../TEST_COVERAGE_PLAN.md)
- **Quick Reference**: [TEST_COVERAGE_QUICK_REF.md](../../../TEST_COVERAGE_QUICK_REF.md)
- **JUnit 5 User Guide**: https://junit.org/junit5/docs/current/user-guide/
- **Mockito Documentation**: https://javadoc.io/doc/org.mockito/mockito-core/latest/
- **AssertJ Documentation**: https://assertj.github.io/doc/
- **Quarkus Testing Guide**: https://quarkus.io/guides/getting-started-testing

---

*Write tests that provide value. Focus on quality, not quantity.*
