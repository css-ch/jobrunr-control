# JobRunr Pro Scheduler Extension - Project Overview

## Architektur

### Hexagonal Architecture (Clean Architecture)

```
┌─────────────────────────────────────────────────────────────┐
│                    DOMAIN LAYER (CORE)                       │
│  - Entities: JobDefinition, ScheduledJobInfo, ...            │
│  - Value Objects: JobParameter, BatchProgress, ...           │
│  - Ports: JobDiscoveryService, JobSchedulerPort, ...         │
│  - Enums: JobStatus, JobParameterType                        │
└──────────────────────┬──────────────────────────────────────┘
                       │ Dependencies point inward
┌──────────────────────▼──────────────────────────────────────┐
│                  APPLICATION LAYER                           │
│  - Use Cases: CreateScheduledJobUseCase, ...                 │
│  - Validation: JobParameterValidator                         │
│  - Orchestration: Koordiniert Domain & Infrastructure        │
└──────────────────────┬──────────────────────────────────────┘
                       │ Uses Ports (Interfaces)
┌──────────────────────▼──────────────────────────────────────┐
│                INFRASTRUCTURE LAYER                          │
│  - Adapters: JobRunrSchedulerAdapter, ...                    │
│  - External Systems: JobRunr Pro, PostgreSQL                 │
│  - Technical Details: Reflection, Caching, HTTP              │
└─────────────────────────────────────────────────────────────┘
```

**Vorteile**:

- ✅ Testbarkeit: Domain & Application isoliert testbar
- ✅ Austauschbarkeit: Infrastructure leicht ersetzbar
- ✅ Klarheit: Eindeutige Abhängigkeitsrichtungen
- ✅ Wartbarkeit: Änderungen bleiben lokal

---

## Build & Run

### Kompilierung

```bash
./mvnw clean compile
# ✅ BUILD SUCCESS (~1.5s)
```

### Development Mode

```bash
./start-postgres.sh
./mvnw quarkus:dev
# Startet auf http://localhost:8080
# JobRunr Dashboard: http://localhost:8000
```

---

## Verwendung

### External Trigger API ✅

**Trigger a job:**

```bash
curl -X POST http://localhost:8080/api/external-trigger/{jobId}/trigger
```

**Check job status:**

```bash
curl http://localhost:8080/api/external-trigger/{jobId}/status
```
 
