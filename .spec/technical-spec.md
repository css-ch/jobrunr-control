# Technische Spezifikation: JobRunr Pro Scheduler Extension

## 1. Technologie-Stack & Versionierung

* **Runtime:** OpenJDK 21 (Nutzung von Records, Pattern Matching, Virtual Threads wenn sinnvoll).
* **Framework:** Quarkus (Latest Stable Release).
* **Job Engine:** JobRunr Pro 8.3 (Integration via Maven/Gradle Dependency).
* **Template Engine:** Quarkus Qute (Type-safe Templates).
* **Frontend Interactivity:** htmx 1.9.x (via Webjar oder CDN).
* **CSS Framework:** Bootstrap 5.3 (via CDN).
* **Datenbank:** PostgreSQL 16.
* **Containerization:** Podman (Development & Testing).

## 2. Architektur-Design (Clean Architecture)

Die Anwendung folgt strikt der **Clean Architecture** (Onion Architecture). Die Abhängigkeitsregel zeigt von außen nach innen.

### 2.1 Layer-Struktur & Package-Naming

```text
src/main/java/com/company/scheduler/
├── domain/                  # Kern-Modelle (POJOs/Records), Keine Framework-Deps
│   ├── model/               # JobDefinition, JobParameter, JobStatus (Enums)
│   └── ports/               # Interfaces für Repositories/Services (Inbound/Outbound)
├── application/             # Anwendungslogik & Use Cases
│   ├── service/             # JobPlanningService, JobSearchService
│   └── dto/                 # Data Transfer Objects für UI (ViewModels)
├── infrastructure/          # Implementierungsdetails
│   ├── jobrunr/             # Implementation der Domain-Ports via JobRunr Pro API
│   └── config/              # Quarkus Konfigurationen (CDI Producers)
└── presentation/            # UI Layer
    ├── controller/          # JAX-RS Ressourcen (Endpoints)
    └── view/                # Qute Template Extensions/Data

```

## 3. Implementierungs-Details

### 3.1 Domain Layer

* Nutzung von **Java Records** für alle unveränderlichen Datenstrukturen (Models).
* Keine Persistenz-Annotationen (JPA) im Domain Layer.
* Mapping-Interfaces definiert, um JobRunr-interne Objekte in Domain-Objekte zu wandeln.

### 3.2 Infrastructure Layer (JobRunr Integration)

* **Dependency Injection:** Injektion der `org.jobrunr.scheduling.JobScheduler` und `org.jobrunr.storage.StorageProvider` Beans via CDI (`@Inject`).
* **JobRunr Pro Features:** Nutzung der `JobRequest` API für typisierte Parameter-Übergabe.
* **Datenbank:** Konfiguration via `application.properties` (`quarkus.datasource...`). Nutzung der existierenden JobRunr-Tabellen; keine eigenen Entity-Klassen für Jobs erstellen.

### 3.3 Presentation Layer (Qute + htmx)

**Server-Side Rendering Strategie:**

* Controller (JAX-RS) geben `io.quarkus.qute.TemplateInstance` zurück.
* Verwendung von **Fragmenten**: Controller liefern bei htmx-Requests (`hx-request: true`) nur HTML-Fragmente (Teilbereiche) zurück, bei initialen Requests die volle Seite (`base.html` + Content).

**htmx Muster:**

* **Navigation:** `hx-get="/jobs"` `hx-target="#main-content"` `hx-push-url="true"`.
* **Suche/Filter:** `hx-trigger="keyup changed delay:500ms, search"` auf Input-Feldern.
* **Modals:** Verwendung von Bootstrap Modals. Inhalt wird dynamisch via `hx-get="/jobs/create-form"` in den Modal-Body geladen.
* **Löschen:** `hx-delete="/jobs/{id}"` mit `hx-confirm="Wirklich löschen?"`.
* **Polling:** Für die Batch-Job-Fortschrittsanzeige (View B) Nutzung von `hx-trigger="every 2s"` auf dem spezifischen Tabellen-Element.

**CSS / Layout:**

* Bootstrap 5 Klassen direkt im HTML.
* Responsive Tables (`table-responsive`).
* Nutzung von Bootstrap Icons (BI).

### 3.4 Asynchrone Verarbeitung (Non-Blocking)

* Da die Analyse von Batch-Jobs (Subjobs) IO-intensiv ist, müssen Controller-Methoden, die diese Daten abrufen, **reaktiv** sein.
* Nutzung von `Uni<TemplateInstance>` (Mutiny) in den Controllern.
* Verwendung von `@Blocking` nur wo unumgänglich (wenn JobRunr API blockierend ist), idealerweise Auslagerung auf Worker-Threads.

## 4. Testing Strategie

### 4.1 Unit Tests

* **Framework:** JUnit 5 + Mockito.
* **Scope:** Testen der `application` Services und Mapper.
* **Mocking:** Mocken der JobRunr `JobScheduler` und `StorageProvider` Interfaces.

### 4.2 Integration Tests

* **Framework:** `@QuarkusTest` + RestAssured.
* **Scope:** Testen der HTTP-Endpoints (Controller) und Qute-Template-Rendering.
* **Container:** Nutzung von **Testcontainers** für PostgreSQL.
* **Podman:** Konfiguration der Testcontainers für Nutzung des Podman Sockets (Environment Check).

```java
// Beispiel Annotation für Integration Test
@QuarkusTest
@Testcontainers
class JobControllerTest {
    // Tests für htmx response headers und HTML body checks
}

```

## 5. Build & Deployment Konfiguration

* **Build Tool:** Maven.
* **Native Image:** Die Anwendung muss `native`-kompilierbar sein (Vermeidung von Reflection wo nicht nötig, Registrierung von Qute Templates für Reflection).
* **Docker/Podman:**
* Multi-Stage Dockerfile (Build in Maven Image, Run in Distroless/UBI Minimal).
* `podman-compose.yml` für lokale Entwicklung (App + Postgres).



## 6. Code Guidelines

* **Constructor Injection:** Verzicht auf `@Inject` an Feldern (Field Injection), Nutzung von Konstruktor-Injection für bessere Testbarkeit.
* **Lombok:** **Nicht** verwenden. Nutzung von Java 17+ Records und Standard Gettern/Settern bzw. IDE-Generierung.
* **Logging:** Nutzung von `jboss-logging` (in Quarkus integriert). Structured Logging Prinzipien beachten.
* **Fehlerbehandlung:** Globale Exception Mappers (`ExceptionMapper`) für UI-Fehlermeldungen (Toast-Notifications via htmx Response Header `HX-Trigger`).

---

