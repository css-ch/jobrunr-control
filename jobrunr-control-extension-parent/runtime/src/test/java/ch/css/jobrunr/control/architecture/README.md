# Architecture Tests

This package contains ArchUnit tests that validate the Clean/Hexagonal Architecture described in
the [arc42 documentation](../../../../../../docs/arc42-architecture.adoc).

## Test Files

### 1. CleanArchitectureTest

Validates the overall Clean Architecture pattern:

- **Layer separation**: Domain, Application, Infrastructure, Adapter layers
- **Dependency rules**: Ensures proper dependency flow (Domain ← Application ← Infrastructure/Adapter)
- **Framework independence**: Domain layer should not depend on frameworks
- **Ports and Adapters**: Validates Hexagonal Architecture pattern
- **No circular dependencies**: Ensures clean package structure

### 2. PlantUmlComponentStructureTest

Validates that the actual code structure matches the PlantUML diagrams in the arc42 documentation:

- **Domain Layer components**: JobDefinition, ScheduledJobInfo, JobParameter, etc.
- **Application Layer components**: Use cases for scheduling, monitoring, discovery
- **Infrastructure Layer components**: Adapters implementing domain ports
- **Adapter Layer components**: Controllers and REST resources

### 3. DesignPatternsTest

Validates design patterns and coding conventions:

- **Naming conventions**: *UseCase, *Adapter, *Controller, *Resource
- **No Lombok**: Java Records are used instead (Java 21 feature)
- **Constructor injection**: No field injection with @Inject
- **JobRequest pattern**: Immutable records
- **ConfigurableJob pattern**: Job discovery mechanism
- **CDI annotations**: Proper use of @ApplicationScoped

## Running the Tests

```bash
# Run all architecture tests
mvn test -Dtest="*ArchitectureTest,*ComponentStructureTest,*PatternsTest"

# Run a specific test class
mvn test -Dtest=CleanArchitectureTest

# Run with debug output
mvn test -Dtest=CleanArchitectureTest -X
```

## Architecture Rules

### Layer Dependencies

```
┌─────────────────┐
│  Adapter Layer  │ (Controllers, REST Resources)
├─────────────────┤
│Infrastructure L.│ (JobRunr Adapters)
├─────────────────┤
│ Application L.  │ (Use Cases)
├─────────────────┤
│   Domain Layer  │ (Entities, Ports)
└─────────────────┘
```

Dependencies flow: **Domain ← Application ← Infrastructure/Adapter**

### Package Structure

- `ch.css.jobrunr.control.domain` - Pure business logic, no framework dependencies
- `ch.css.jobrunr.control.application` - Use cases organized by capability
- `ch.css.jobrunr.control.infrastructure` - Technical implementations (JobRunr adapters)
- `ch.css.jobrunr.control.adapter` - UI (JAX-RS controllers) and REST (API resources)
- `ch.css.jobrunr.control.annotations` - Custom annotations

### Naming Conventions

- Use Cases: `*UseCase` (e.g., `CreateScheduledJobUseCase`)
- Adapters: `*Adapter` (e.g., `JobRunrSchedulerAdapter`)
- Controllers: `*Controller` (e.g., `ScheduledJobsController`)
- REST Resources: `*Resource` (e.g., `ExternalTriggerResource`)
- Domain Ports: `*Port` or `*Service` (e.g., `JobSchedulerPort`)

## Known Architecture Violations (To Be Fixed)

The following tests detect actual violations that should be addressed:

1. **Field Injection in Controllers and Adapters**
    - `ScheduledJobsController` - uses @Inject on fields
    - `JobExecutionsController` - uses @Inject on fields
    - `JobDefinitionDiscoveryAdapter` - uses @Inject on fields
    - **Action**: Refactor to use constructor injection

These tests are currently disabled (commented out) but should be enabled once the violations are fixed.

## Relationship to arc42 Documentation

These tests are derived from and validate:

- **Section 5: Building Block View** - Component structure from PlantUML diagrams
- **Section 8: Cross-cutting Concepts** - Design patterns and conventions
- **Section 2: Architecture Constraints** - Technical and organizational constraints

The tests ensure that the implementation matches the documented architecture and that architectural decisions are
enforced automatically during the build.
