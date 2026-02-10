# Copilot & Contributor Instructions: JobRunr Control

This document provides mandatory context and constraints for the **JobRunr Control Quarkus Extension**. Follow these
instructions to maintain architectural integrity and idiomatic consistency.

## 1. System Context & Source of Truth

- **Primary Reference:** Use `docs/arc42.adoc` for all architectural decisions, building blocks, and system constraints.

- **Tech Stack:**
    - Java 21+
    - Quarkus 3.30.8
    - JobRunr Pro 8.4.1 (Community Edition is strictly forbidden)

- **Library Metadata:** Use Context7 MCP to resolve library details for versions specified in `arc42.adoc`.

- **Sync note:** When you change these contributor instructions, also update `docs/programmers.adoc` and
  `docs/user.adoc` to keep developer guidance and user-facing documentation consistent. Add a short changelog entry in
  those files explaining the update.

## 2. Architectural Guardrails (Hexagonal Architecture)

Strictly adhere to the separation of concerns defined in **AD-1**:

- **Domain Layer:**
    - Pure Java only. **No framework dependencies** (Quarkus, JAX-RS, Hibernate).
    - Contains entities (`JobDefinition`), value objects (`JobParameter`), and Ports (Interfaces).

- **Application Layer:** Orchestrates use cases (e.g., `CreateTemplateUseCase`, `JobParameterValidator`).

- **Adapter Layer (Inbound/Outbound):**
    - **Inbound:** REST API (JAX-RS) and Web UI (Qute + HTMX).
    - **Outbound:** Implementations for JobRunr Pro, Hibernate, and Quarkus Config.

- **Infrastructure:** Implement Ports defined in the Domain layer.

## 3. Implementation Patterns

### Job Discovery & Pattern

- **JobRequest Pattern Only:** Every configurable job MUST use the `JobRequest` / `JobRequestHandler` pattern. *
  *Lambda-based jobs are forbidden**.

- **Build-Time Discovery:** Jobs must be annotated with `@ConfigurableJob` on the `run()` method for build-time scanning
  via Jandex.

### Coding Standards

- **Data Structures:** Use **Java Records** for all `JobRequest` implementations.

- **Typing:** Prefer strictly typed fields (Enums, `LocalDate`, `Integer`) over raw Strings.

- **Security:** Apply `@RolesAllowed` (`viewer`, `configurator`, `admin`) to all endpoints.

### Parameter Storage (AD-2)

- **Inline (Default):** For standard parameters.

- **External Storage:** Use `@JobParameterSet` for large text or numerous parameters. Requires Hibernate ORM.

## 4. Logging Standards (Required)

We use **JBoss Logging** exclusively. Use formatted methods to avoid string concatenation.

- **Initialization:**

```java
private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(MyClass.class);
```

- **Methods:** Use `.infof()`, `.errorf()`, etc.

- **Exceptions:** Always pass the exception as the first argument: `LOG.errorf(e, "Message %s", id);`.

## 5. Testing & Native Image

- **Testability:** Inject Ports (interfaces), never concrete adapters.

- **Native Mode:** Avoid reflection-heavy patterns. While not currently tested, do not break potential native
  compilation.

## 6. Documentation Style

- **Language:** English only.

- **Public API:** Document only public classes/interfaces.

- **Traceability:** Always include identifiers like `jobId` or `requestId` in logs and comments.

- **Syncing documentation:** When these contributor instructions change in a way that affects developer practices, UI
  behavior, or public APIs, update `docs/programmers.adoc` and `docs/user.adoc` accordingly and add a brief changelog
  note describing the changes and rationale.
