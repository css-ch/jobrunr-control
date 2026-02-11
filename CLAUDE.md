Use @docs/arc42.adoc as the architecture reference.
Use context7 MCP for library documentation (versions in arc42).

# Project

Quarkus extension for JobRunr Pro. Provides a web-based control dashboard and REST API for job scheduling, monitoring,
batch processing, and external triggers.

- Java 21, Quarkus 3.31.2, JobRunr Pro 8.4.2
- Maven build with `./mvnw`
- Server-side UI with Qute templates + HTMX

# Architecture

Clean Architecture (Hexagonal) — enforced by ArchUnit tests:

- **Domain** (`domain/`): Records, ports (interfaces), exceptions. No framework dependencies.
- **Application** (`application/`): Use cases with `execute()` method. Each operation is a separate `@ApplicationScoped`
  class.
- **Adapter** (`adapter/`): REST API (`adapter/rest/`) and UI (`adapter/ui/`).
- **Infrastructure** (`infrastructure/`): Implements domain ports. JobRunr integration, JPA persistence, Flyway
  migrations.

# Build Commands

- Compile: `./mvnw clean compile`
- Full build with tests: `./mvnw clean verify`
- Dev mode: `./mvnw -f jobrunr-control-example/pom.xml quarkus:dev`

# Code Conventions

- Use Java records for immutable DTOs and value objects
- Constructor injection with `@Inject`
- Use Case naming: `[Action][Entity]UseCase` (e.g., `StartJobUseCase`)
- Adapter naming: `[Entity][Purpose]Adapter` (e.g., `JobRunrSchedulerAdapter`)
- Port naming: `[Entity][Action]Port` (e.g., `JobSchedulerPort`)

# Documentation

- Only document public APIs
- Use English for documentation

# Logging

- Always use JBoss Logging: `org.jboss.logging.Logger`
- Logger constant MUST be uppercase: `private static final Logger LOG = Logger.getLogger(ClassName.class);`
- Use formatted logging methods: `LOG.infof()`, `LOG.warnf()`, `LOG.debugf()`, `LOG.errorf()`
- Include exception as first parameter when logging errors: `LOG.errorf(exception, "Message: %s", context)`

# Testing

- JUnit 5 + Mockito + AssertJ
- `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`
- Use `@DisplayName` for test descriptions
- Architecture tests with ArchUnit enforce layer separation
- JaCoCo for coverage, SpotBugs for static analysis

# Security

- Role-based access: `@RolesAllowed` with roles `api-reader`, `api-executor`, `admin`
- REST endpoints use OpenAPI annotations (`@Operation`, `@APIResponse`)
