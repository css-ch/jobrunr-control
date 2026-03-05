# Security Specification – JobRunr Control

## Overview

JobRunr Control secures three distinct HTTP surfaces:

| Surface | Base Path | Authentication |
|---|---|---|
| Web UI | `/q/jobrunr-control`, `/q/jobrunr-control/*` | OIDC session cookie (Authorization Code Flow) |
| JobRunr Pro Dashboard | `/q/jobrunr`, `/q/jobrunr/*` | OIDC session cookie (same as Web UI) |
| REST API | `/q/jobrunr-control/api/*` | Bearer token (OIDC Client Credentials) |

Access control is enforced at two complementary layers:

1. **Quarkus HTTP Auth Policies** – path + method matching before a request reaches any handler
2. **`@RolesAllowed`** – method-level enforcement inside JAX-RS resources

Both layers must pass. HTTP policies act as the outer gate; `@RolesAllowed` acts as the inner guard.

---

## Roles

### Web UI Roles

| Role | Permissions |
|---|---|
| `viewer` | Read access to scheduled jobs and execution history |
| `configurator` | `viewer` + create, edit, delete jobs and templates |
| `admin` | `configurator` + immediate job execution |

### REST API Roles

| Role | Permissions |
|---|---|
| `api-reader` | GET endpoints (status queries) |
| `api-executor` | `api-reader` + POST endpoints (start jobs) |
| `admin` | Full access to all REST API operations |

> `admin` is shared across both surfaces.

---

## Endpoint Permissions

### Web UI (`/q/jobrunr-control`)

| Roles | HTTP | Path | Method |
|---|---|---|---|
| `viewer`, `configurator`, `admin` | GET | `/` | `DashboardController.index()` |
| `viewer`, `configurator`, `admin` | GET | `/scheduled/` | `ScheduledJobsController.getScheduledJobsView()` |
| `viewer`, `configurator`, `admin` | GET | `/scheduled/table` | `ScheduledJobsController.getScheduledJobsTable()` |
| `viewer`, `configurator`, `admin` | GET | `/scheduled/modal/parameters` | `ScheduledJobsController.getJobParameters()` |
| `configurator`, `admin` | GET | `/scheduled/modal/new` | `ScheduledJobsController.getNewJobModal()` |
| `configurator`, `admin` | GET | `/scheduled/modal/{id}/edit` | `ScheduledJobsController.getEditJobModal()` |
| `configurator`, `admin` | POST | `/scheduled/` | `ScheduledJobsController.createJob()` |
| `configurator`, `admin` | PUT | `/scheduled/{id}` | `ScheduledJobsController.updateJob()` |
| `configurator`, `admin` | DELETE | `/scheduled/{id}` | `ScheduledJobsController.deleteJob()` |
| **`admin` only** | POST | `/scheduled/{id}/execute` | `ScheduledJobsController.executeJob()` |
| `viewer`, `configurator`, `admin` | GET | `/history/` | `JobExecutionsController.getExecutionHistoryView()` |
| `viewer`, `configurator`, `admin` | GET | `/history/table` | `JobExecutionsController.getExecutionHistoryTable()` |
| `viewer`, `configurator`, `admin` | GET | `/history/{id}/batch-progress` | `JobExecutionsController.getBatchProgressFragment()` |
| `viewer`, `configurator`, `admin` | GET | `/templates/` | `TemplatesController.getTemplatesView()` |
| `viewer`, `configurator`, `admin` | GET | `/templates/table` | `TemplatesController.getTemplatesTable()` |
| `viewer`, `configurator`, `admin` | GET | `/templates/modal/parameters` | `TemplatesController.getJobParameters()` |
| `configurator`, `admin` | GET | `/templates/modal/new` | `TemplatesController.getNewTemplateModal()` |
| `configurator`, `admin` | GET | `/templates/modal/{id}/edit` | `TemplatesController.getEditTemplateModal()` |
| `configurator`, `admin` | POST | `/templates/` | `TemplatesController.createTemplate()` |
| `configurator`, `admin` | PUT | `/templates/{id}` | `TemplatesController.updateTemplate()` |
| `configurator`, `admin` | DELETE | `/templates/{id}` | `TemplatesController.deleteTemplate()` |
| `configurator`, `admin` | POST | `/templates/{id}/clone` | `TemplatesController.cloneTemplate()` |
| **`admin` only** | POST | `/templates/{id}/start` | `TemplatesController.startTemplate()` |

### Embedded JobRunr Pro Dashboard (`/q/jobrunr`)

The embedded JobRunr Pro dashboard has a **separate, intentionally restricted** access model.
Unlike the custom Web UI, it does not support fine-grained role-based write operations.
Therefore access is limited to two levels:

| Roles | HTTP | Path | Description |
|---|---|---|---|
| `viewer`, `configurator`, `admin` | GET, OPTIONS, HEAD | `/q/jobrunr/api/*` | Read-only access |
| **`admin` only** | POST, PUT, DELETE, PATCH | `/q/jobrunr/api/*` | Write access |

> **Rationale:** `configurator` intentionally has **no write access** to the embedded JobRunr
> Pro dashboard. The dashboard does not support fine-grained operation-level control (e.g.
> "restart job" vs. "delete job"), so write access is restricted to `admin` only to prevent
> unintended destructive operations by `configurator` users.

#### Required: `JobRunrDashboardUserContextFilter`

When `type=embedded` is used, JobRunr Pro delegates authentication to a CDI bean of type
`JobRunQuarkusAuthenticationFilter`. Without a registered bean, no `JobRunrUser` is ever
placed in the Vert.x request context, and the dashboard frontend shows
**"You do not have access to the JobRunr Pro Dashboard"** for every request.

The extension provides `JobRunrDashboardUserContextFilter` (registered automatically as a
CDI bean) which sets a `JobRunrUser` with `allowAll()` authorisation rules for every request:

```java
// ch.css.jobrunr.control.security.JobRunrDashboardUserContextFilter
@ApplicationScoped
public class JobRunrDashboardUserContextFilter implements JobRunQuarkusAuthenticationFilter {
    private static final JobRunrUser ALLOW_ALL_USER =
            new JobRunrUser(null, null, JobRunrUserAuthorizationRules.allowAll());

    @Override
    public void filter(RoutingContext ctx) {
        if (!JobRunrUserContext.hasCurrentUser()) {
            JobRunrUserContext.setCurrentUser(ALLOW_ALL_USER);
        }
        ctx.next();
    }
}
```

This is **not** a security hole: `allowAll()` only satisfies JobRunr Pro's internal
authorisation check. All actual access control is enforced by the Quarkus HTTP Auth
Policies (`jobrunr-api-read` / `jobrunr-api-write`) before the request reaches this
filter. JobRunr Pro never sees an unauthenticated or unauthorised request.

### REST API (`/q/jobrunr-control/api`)

| Roles | HTTP | Path | Method |
|---|---|---|---|
| `api-reader`, `api-executor`, `admin` | GET | `/jobs/{jobId}` | `JobControlResource.getJobStatus()` |
| `api-executor`, `admin` | POST | `/jobs/{jobRef}/start` | `JobControlResource.startJob()` |

> `{jobRef}` accepts a UUID or a template name. UUID format is detected automatically; otherwise the value is treated as a template name (templates only).

**Error responses for `/jobs/{jobRef}/start`:**

| Status | Condition |
|---|---|
| 200 | Job started successfully |
| 404 | No job/template found for the given UUID or name |
| 409 | Template name already exists (on create/update, not on start) |

---

## OIDC Tenant Architecture

Quarkus is used as a **Backend-for-Frontend (BFF)**. The browser never holds a bearer token;
it authenticates via the OIDC Authorization Code Flow and receives a session cookie.
External service accounts (CI/CD pipelines, automation) use Client Credentials (bearer tokens).

Two OIDC tenants handle these two authentication models:

### Default Tenant (Web UI + JobRunr Dashboard)

```
application-type = web-app
Paths: everything except /q/jobrunr-control/api/*
```

- Handles Authorization Code Flow: redirects to Keycloak login, stores token state in encrypted session cookie.
- Used for all browser traffic to both the Web UI and the embedded JobRunr Pro Dashboard.
- The JobRunr Pro Dashboard JS makes XHR requests to `/q/jobrunr/api/*` — these are same-origin requests from the browser and carry the session cookie. They must use the default `web-app` tenant, **not** a bearer-token tenant.

### `bearer` Tenant (REST API only)

```
application-type = service
Paths: /q/jobrunr-control/api/* (via tenant-paths)
```

- Validates Bearer tokens only (no redirect to login page).
- Restricted to `/q/jobrunr-control/api/*` via `quarkus.oidc.bearer.tenant-paths`.

> **Naming Warning:** The bearer tenant is deliberately named `bearer`, **not** `api`.
> Quarkus OIDC has a **convention-based fallback** in `StaticTenantResolver` that scans URL
> path segments for known tenant IDs. A tenant named `api` would match every URL containing
> `/api/` as a path segment — including `/q/jobrunr/api/*` — overriding the session-cookie
> tenant and causing 401 errors on the JobRunr Pro Dashboard XHR requests.

---

## Role Extraction from Keycloak

Keycloak encodes **realm-level roles** in the access token at `realm_access/roles`.

Quarkus OIDC does **not** check this path by default. Without explicit configuration it
only looks at `groups` and `resource_access/<client-id>/roles` (client-level roles). If
neither path exists in the token, all role checks fail silently and every protected
endpoint returns 401 or 403.

**Required configuration for both tenants:**

```properties
quarkus.oidc.roles.source=accesstoken
quarkus.oidc.roles.role-claim-path=realm_access/roles
quarkus.oidc.bearer.roles.source=accesstoken
quarkus.oidc.bearer.roles.role-claim-path=realm_access/roles
```

> **Debug hint:** If you see these log messages, the role path is not configured correctly:
> ```
> DEBUG OidcUtils: No claim exists at the path 'groups' at the path segment 'groups'
> DEBUG OidcUtils: No claim exists at the path 'resource_access/jobrunr-control/roles'
> ```

---

## Quarkus Profiles

### `default` (Production – with OIDC)

OIDC is **active**. Authentication via Keycloak using Authorization Code Flow for the UI
and Bearer Token for the REST API. The `JobRunrControlRoleAugmentor` is a no-op because
`quarkus.oidc.enabled=true`.

```properties
# Default OIDC tenant (web-app: Authorization Code Flow + session cookie)
quarkus.oidc.auth-server-url=https://your-keycloak.com/realms/your-realm
quarkus.oidc.client-id=jobrunr-control
quarkus.oidc.credentials.secret=${OIDC_CLIENT_SECRET}
quarkus.oidc.application-type=web-app
quarkus.oidc.token-state-manager.encryption-secret=${SESSION_ENCRYPTION_SECRET}
quarkus.oidc.roles.source=accesstoken
quarkus.oidc.roles.role-claim-path=realm_access/roles
quarkus.oidc.token.refresh-expired=true
quarkus.oidc.logout.path=/q/jobrunr-control/logout
quarkus.oidc.logout.post-logout-path=/q/jobrunr-control/scheduled

# Bearer tenant (service: validates bearer tokens for REST API only)
# Named 'bearer' not 'api' — see OIDC Tenant Architecture above
quarkus.oidc.bearer.tenant-paths=/q/jobrunr-control/api/*
quarkus.oidc.bearer.auth-server-url=https://your-keycloak.com/realms/your-realm
quarkus.oidc.bearer.client-id=jobrunr-control
quarkus.oidc.bearer.credentials.secret=${OIDC_CLIENT_SECRET}
quarkus.oidc.bearer.application-type=service
quarkus.oidc.bearer.roles.source=accesstoken
quarkus.oidc.bearer.roles.role-claim-path=realm_access/roles

# Web UI: require authenticated session
quarkus.http.auth.permission.jobrunr-control.paths=/q/jobrunr-control,/q/jobrunr-control/*
quarkus.http.auth.permission.jobrunr-control.policy=authenticated

# REST API: split by HTTP method
quarkus.http.auth.permission.jobrunr-control-api-read.paths=/q/jobrunr-control/api/*
quarkus.http.auth.permission.jobrunr-control-api-read.policy=api-reader-policy
quarkus.http.auth.permission.jobrunr-control-api-read.methods=GET,OPTIONS,HEAD
quarkus.http.auth.permission.jobrunr-control-api-write.paths=/q/jobrunr-control/api/*
quarkus.http.auth.permission.jobrunr-control-api-write.policy=api-executor-policy
quarkus.http.auth.permission.jobrunr-control-api-write.methods=POST,PUT,DELETE,PATCH

# JobRunr Pro Dashboard: protect via Quarkus OIDC (embedded mode bypasses JobRunr's own auth)
quarkus.http.auth.permission.jobrunr-dashboard.paths=/q/jobrunr,/q/jobrunr/*
quarkus.http.auth.permission.jobrunr-dashboard.policy=authenticated

# JobRunr Pro API: split by HTTP method
quarkus.http.auth.permission.jobrunr-api-read.paths=/q/jobrunr/api/*
quarkus.http.auth.permission.jobrunr-api-read.policy=viewer-policy
quarkus.http.auth.permission.jobrunr-api-read.methods=GET,OPTIONS,HEAD
quarkus.http.auth.permission.jobrunr-api-write.paths=/q/jobrunr/api/*
quarkus.http.auth.permission.jobrunr-api-write.policy=admin-policy
quarkus.http.auth.permission.jobrunr-api-write.methods=POST,PUT,DELETE,PATCH

# Custom role policies
quarkus.http.auth.policy.admin-policy.roles-allowed=admin
quarkus.http.auth.policy.viewer-policy.roles-allowed=viewer,configurator,admin
quarkus.http.auth.policy.api-executor-policy.roles-allowed=api-executor,admin
quarkus.http.auth.policy.api-reader-policy.roles-allowed=api-reader,api-executor,admin
```

> **Note:** `viewer-policy` allows `viewer`, `configurator`, and `admin`. `admin-policy`
> restricts write operations to `admin` only. The `jobrunr-api-read/write` permissions are
> the **sole security boundary** for the embedded dashboard – JobRunr Pro does not evaluate
> `@RolesAllowed`. In `dev,keycloak` mode, explicit `%keycloak.*` overrides are required to
> prevent `%dev.permit` from bleeding in (see below).

### `dev` (Development – no Keycloak)

OIDC is **disabled**. The `JobRunrControlRoleAugmentor` is active and grants the `admin`
role to every request. No Keycloak instance required – zero-configuration start for
local development.

```
./mvnw -f jobrunr-control-example/pom.xml quarkus:dev
```

```properties
%dev.quarkus.oidc.enabled=false
%dev.quarkus.http.auth.permission.jobrunr-control.policy=permit
%dev.quarkus.http.auth.permission.jobrunr-dashboard.policy=permit
%dev.quarkus.http.auth.permission.jobrunr-control-api-read.policy=permit
%dev.quarkus.http.auth.permission.jobrunr-control-api-write.policy=permit
%dev.quarkus.http.auth.permission.jobrunr-api-read.policy=permit
%dev.quarkus.http.auth.permission.jobrunr-api-write.policy=permit
```

> **Warning:** Never use the plain `dev` profile in production. All requests are treated
> as `anonymous-user` with `admin` access.

### `dev` + `keycloak` (Development – with external Keycloak)

OIDC is **active** with an external Keycloak instance. The `JobRunrControlRoleAugmentor`
is a **no-op** because `quarkus.oidc.enabled=true`. Real authentication and role mapping
via Keycloak apply.

```
./mvnw -f jobrunr-control-example/pom.xml quarkus:dev -Dquarkus.profile=dev,keycloak
```

```properties
%keycloak.quarkus.keycloak.devservices.enabled=false
%keycloak.quarkus.oidc.enabled=true
%keycloak.quarkus.http.auth.permission.jobrunr-control.policy=authenticated
%keycloak.quarkus.http.auth.permission.jobrunr-dashboard.policy=authenticated
%keycloak.quarkus.http.auth.permission.jobrunr-control-api-read.policy=api-reader-policy
%keycloak.quarkus.http.auth.permission.jobrunr-control-api-write.policy=api-executor-policy
# Restores role enforcement on /q/jobrunr/api/* (overrides %dev.permit bleed-through)
# read: viewer+configurator+admin, write: admin only
%keycloak.quarkus.http.auth.permission.jobrunr-api-read.policy=viewer-policy
%keycloak.quarkus.http.auth.permission.jobrunr-api-write.policy=admin-policy
```

> **Note:** The `%keycloak.*` overrides for `jobrunr-api-read` and `jobrunr-api-write` are
> required. Without them, the `%dev.permit` values bleed into `dev,keycloak` mode (Quarkus
> applies all active profile overrides simultaneously), allowing any authenticated user —
> including `viewer` — to call destructive write endpoints on `/q/jobrunr/api/*`.

### Production without OIDC

Security is **fully disabled**. Intended for deployments in closed internal networks where
no identity provider is available or required. The `JobRunrControlRoleAugmentor` grants
the `admin` role to every request, satisfying all `@RolesAllowed` checks.

```properties
quarkus.oidc.enabled=false
quarkus.http.auth.permission.jobrunr-control.paths=/q/jobrunr-control,/q/jobrunr-control/*
quarkus.http.auth.permission.jobrunr-control.policy=permit
quarkus.http.auth.permission.jobrunr-dashboard.paths=/q/jobrunr,/q/jobrunr/*
quarkus.http.auth.permission.jobrunr-dashboard.policy=permit
quarkus.http.auth.permission.jobrunr-control-api-read.paths=/q/jobrunr-control/api/*
quarkus.http.auth.permission.jobrunr-control-api-read.policy=permit
quarkus.http.auth.permission.jobrunr-control-api-write.paths=/q/jobrunr-control/api/*
quarkus.http.auth.permission.jobrunr-control-api-write.policy=permit
```

> **Note:** Network-level access control (firewall, VPN, reverse proxy) is the operator's
> responsibility in this configuration. The application itself performs no authentication.

---

## JobRunrControlRoleAugmentor

**Location:** `ch.css.jobrunr.control.security.JobRunrControlRoleAugmentor`
**Implements:** `io.quarkus.security.identity.SecurityIdentityAugmentor`

### Purpose

Bridges the gap between disabled OIDC and `@RolesAllowed`-protected endpoints.
Without it, any request with `quarkus.oidc.enabled=false` would result in `403 Forbidden`
because Quarkus finds no roles on the anonymous identity.

### Activation

The augmentor is always compiled in (no build-profile restriction). Its behaviour is
determined solely by `quarkus.oidc.enabled` at runtime:

| Scenario | `quarkus.oidc.enabled` | Effect |
|---|---|---|
| `dev` (no keycloak) | `false` | Creates synthetic principal `anonymous-user`, grants `admin` |
| `dev,keycloak` | `true` | Returns the identity unchanged – Keycloak handles everything |
| Production with OIDC | `true` | Returns the identity unchanged – Keycloak handles everything |
| Production without OIDC | `false` | Creates synthetic principal `anonymous-user`, grants `admin` |

### Why `admin` only

`admin` is included in every `@RolesAllowed` list in the extension. Granting only
`admin` in dev mode is therefore sufficient and avoids granting individual roles
(`viewer`, `configurator`, `api-reader`, `api-executor`) that would only add noise.

### Why in the Runtime module (not only in the Example)

The augmentor uses the extension's internal role names as part of its contract.
Placing it only in the example project would require every extension user to copy it
with knowledge of those internal names. In the runtime module it provides a
zero-configuration dev experience for all users of the extension.

### Single-Warning Design

An `AtomicBoolean warningLogged` flag ensures the security warning is logged exactly
once per application start, not on every request:

```
WARN: OIDC disabled: Granting admin role to all requests. Ensure this is intentional!
```

---

## Security Defaults

| Concern | Default | Override |
|---|---|---|
| OIDC authentication | **enabled** | `quarkus.oidc.enabled=false` |
| UI path protection | `authenticated` policy | Configurable per profile |
| API read protection | `api-reader-policy` | Configurable per profile |
| API write protection | `api-executor-policy` | Configurable per profile |
| JobRunr dashboard read access | `viewer-policy` | `viewer`, `configurator`, `admin` |
| JobRunr dashboard write access | `admin-policy` | `admin` only |
| Role augmentation | No-op (OIDC active) | Active when OIDC disabled |
| Bearer tenant name | `bearer` | Do not rename to `api` (path-segment collision) |
| Realm role path | `realm_access/roles` | Keycloak realm roles location in JWT |

Secure by default: disabling authentication requires explicit opt-in configuration.

---

## Debugging Security Issues

### Enable security trace logging

Add to `application.properties` scoped to the relevant profile:

```properties
%keycloak.quarkus.log.category."io.quarkus.security".level=DEBUG
%keycloak.quarkus.log.category."io.quarkus.oidc".level=DEBUG
%keycloak.quarkus.log.category."io.quarkus.vertx.http.security".level=DEBUG
%keycloak.quarkus.log.category."ch.css.jobrunr.control.security".level=DEBUG
```

> **Warning:** Never add these without a profile prefix. They will enable verbose security
> logging in production, exposing token contents in log files.

### Common symptoms

| Symptom | Likely cause | Fix |
|---|---|---|
| 401 on `/q/jobrunr/api/*` with `Bearer access token is not available` | OIDC tenant named `api` hijacks path segment | Rename tenant to `bearer` |
| 401/403 on all endpoints, log shows `No claim exists at the path 'groups'` | Missing `role-claim-path` | Add `quarkus.oidc.roles.role-claim-path=realm_access/roles` |
| "You do not have access to the JobRunr Pro Dashboard" | `JobRunrDashboardUserContextFilter` not registered | Verify extension is on classpath |
| 403 in dev mode with OIDC disabled | `JobRunrControlRoleAugmentor` not activating | Check `quarkus.oidc.enabled=false` is set |
| `%dev.permit` bleeds into `dev,keycloak` | Missing `%keycloak.*` overrides for jobrunr-api permissions | Add `%keycloak.quarkus.http.auth.permission.jobrunr-api-*.policy` overrides |

---

## Changelog

### 2026-03-05: Template name uniqueness, REST name-based start, UI error fix, config warnings

- **Template name uniqueness**: Template names are now unique system-wide. `CreateTemplateUseCase` and `UpdateTemplateUseCase` check for duplicates and throw `DuplicateTemplateNameException` (→ HTTP 409). `JobRunrSchedulerAdapter` enforces the same constraint at the persistence layer as a second barrier.
- **Clone auto-naming**: Cloning a template generates a unique name automatically (`{name}-1`, `{name}-2`, …) to avoid conflicts.
- **REST start by name**: `POST /jobs/{jobRef}/start` now accepts either a UUID or a template name. UUID detection is format-based; name-based lookup searches templates only.
- **UI error response fix**: `BaseController.buildErrorResponse` now uses `HX-Retarget`/`HX-Reswap` headers instead of `hx-swap-oob`. The previous approach caused HTMX to replace `#jobs-table` with empty content on error, breaking subsequent form submissions.
- **Extension config registration**: `JobRunrControlUiConfig` now carries `@ConfigRoot(phase = ConfigPhase.RUN_TIME)`, eliminating the `Unrecognized configuration key "quarkus.jobrunr-control.ui.show-job-uuid"` warning.
- **Logging min-level**: Added `quarkus.log.category."org.jobrunr".min-level=TRACE` to the example `application.properties` to eliminate the build-time promotion warning for TRACE log level.

### 2026-03-04: Dual-tenant OIDC architecture, role-claim-path, debug log cleanup

- Documented the BFF pattern: default `web-app` tenant for browser traffic, `bearer` tenant for REST API service accounts
- Bearer tenant named `bearer` (not `api`) to avoid `StaticTenantResolver` path-segment collision on `/q/jobrunr/api/*`
- Added `quarkus.oidc.roles.role-claim-path=realm_access/roles` for both tenants — required for Keycloak realm roles
- Added OIDC Tenant Architecture section explaining the two-tenant design and the naming constraint
- Added Role Extraction from Keycloak section with debug hint for missing claim paths
- Added Debugging Security Issues section with symptom/cause/fix table
- Fixed production-scope debug logging: removed unprefixed `io.quarkus.vertx.http.security` and `io.quarkus.security` DEBUG lines from `application.properties`
- Added Production without OIDC profile section
- Updated Security Defaults table with `bearer` tenant name and `realm_access/roles` entries

### 2026-02-21: Conditional OIDC Security

- Added `JobRunrControlRoleAugmentor` for development/testing without OIDC
- Set `quarkus.oidc.enabled=false` to disable authentication (dev/test only)
- UI automatically hides logout/user info when OIDC is disabled
- Security by default: OIDC remains enabled unless explicitly disabled
