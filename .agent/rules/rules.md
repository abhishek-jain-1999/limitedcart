---
trigger: always_on
---


# LimitedCart Engineering Rules

Production-grade guardrails for the LimitedCart platform. Apply them to every change, and challenge deviations with clear technical justification.

## Backend (Domain Services & Workers)
- Preserve bounded contexts: shared types live in `common`; no service reaches into another service’s database or publishes unversioned events.
- APIs are contract-first: DTOs in `dto/` define public payloads, are versioned, and must be covered by contract tests (REST + Kafka + Temporal workflow stubs).
- All persistence changes flow through Flyway migrations committed with forward-only scripts; include rollback/cleanup procedures in the PR description.
- Saga + messaging rules: Kafka producers publish with correlation IDs, manual acks, and idempotency keys; Temporal workflows uphold compensating actions for every activity.
- Reliability first: enforce optimistic locking (`@Version`), retries with bounded backoff, and circuit breakers on every cross-service call (HTTP, Kafka, Redis, Temporal).
- Performance budgets: p95 API latency < 75 ms and consumer lag < 100 messages; load/regression tests must run before releases touching the hot path (orders, payments, inventory).
- Observability baked in: emit structured logs, Micrometer metrics, and trace spans for each order journey step to Zipkin/OpenTelemetry.
- Testing pyramid: unit (Kotlin/JUnit5), service (Spring + Testcontainers), workflow tests (Temporal SDK), and end-to-end smoke flows triggered via CI.
- High-concurrency patterns: API endpoints for hot paths (e.g., flash sales) must use async handoff (Redis + Kafka) instead of direct DB/Workflow writes; synchronous processing is forbidden on the critical path.
-Idempotency: Mandatory for all Kafka consumers and Temporal activities to handle at-least-once delivery; consumers must check existence before processing.

## Spring Boot Services
- Kotlin conventions: favor immutable `data class` DTOs, extension functions for utilities, and `sealed` hierarchies for domain status types; nullability is explicit.
- Configuration hygiene: all values resolve from `.env` → `application.yml` → profile overrides; no hard-coded ports, secrets, or URLs in code.
- Security baseline: JWT validation enforced via shared filters, role annotations used for admin/warehouse endpoints, and sensitive headers redacted from logs.
- HTTP clients: use Spring `RestClient/WebClient` with 2s connect/5s read timeouts, retry policies that respect idempotency, and correlation ID propagation.
- Kafka & Temporal: producers + consumers are transactional, use error-handling topics, and expose lag metrics; Temporal workers register versioned workflows with deterministic code paths.
- Database access: repositories are the only persistence entry point; transactions wrap service methods, and pagination/streaming is mandatory on read-heavy endpoints.
- Health + readiness: every service exposes `/actuator/health`, `/actuator/metrics`, and `/actuator/prometheus`; readiness fails closed when dependencies are unavailable.
- Automation: `mvn clean verify` with Kotlin compiler warnings as errors gate every merge; dependency updates require SBOM + vulnerability scan.
-Redis usage: Atomic Lua scripts are mandatory for "check-then-act" logic (e.g., inventory reservation); never rely on application-level locks or simple get/set sequences for shared counters.
-Async Gatekeeper: For >1k RPS endpoints, split logic into "Producer" (HTTP thread, <10ms) and "Consumer" (Worker thread); never block HTTP threads on Temporal workflow starts (~50ms latency).

## Frontend-Web (React + TypeScript SPA)
- Use TypeScript everywhere; declare API models in `src/types` and ensure they mirror backend DTOs via generated or shared schemas.
- Component architecture: smart/container pages under `pages/`, pure components under `components/`, cross-cutting logic in `hooks/` and `contexts/`.
- State + networking: `AuthContext` is the single source of JWT truth; API calls live in `services/` and must centralize base URLs, interceptors, and SSE handling.
- Security: tokens stay in HttpOnly cookies or encrypted storage; never interpolate untrusted data into DOM; guard routes with role-based wrappers.
- UX/performance: Tailwind is the design system; enforce responsive breakpoints, skeleton states, and SSE reconnection backoff for real-time order progress.
- Testing: write component tests with Vitest/React Testing Library for critical flows (auth, checkout, payments) and Cypress smoke specs for end-to-end regressions.
- Build hygiene: `npm run lint && npm run test` must pass; environment variables prefixed with `VITE_`, and `VITE_API_BASE_URL` defaults to the gateway when unset.

## Flutter Web App (Customer + Admin + Warehouse)
- Architecture: keep `core/`, `features/`, and `shared/` boundaries; all business state flows through `Provider/ChangeNotifier` instances registered in `main.dart`.
- Routing: GoRouter is the single routing source; centralize role-based redirects and deep links (payment tokens) in one guard middleware.
- API access: wrap Dio in `core/api_client.dart`, inject interceptors for JWT + correlation IDs, and normalize error handling to typed failures.
- State management: never mutate providers outside `notifyListeners`; expose read-only models to widgets and keep async operations cancel-safe.
- UI/UX: responsive design (LayoutBuilder/breakpoints) and shared widgets for tables/forms; admin + warehouse layouts reuse navigation shells for accessibility.
- Testing: widget tests for each dashboard page, provider tests for business logic, and golden tests for UI-critical flows.
- Performance + size: tree-shake icons/assets, enable deferred loading for large admin modules, and measure Lighthouse scores before GA releases.

## Docker & Local Infrastructure
- Use multi-stage Dockerfiles (already provided for services) to keep images minimal; pin base image digests and run as non-root.
- `docker-compose.yml` is the source of truth for local stacks (Postgres, Kafka, Redis, Temporal, Zipkin, nginx); keep it updated with new services and health checks.
- Secrets flow from `.env` into compose; never bake credentials into images. Provide `.env.example` for onboarding.
- Container health: each service implements `HEALTHCHECK` commands; compose waits for infrastructure readiness before workers/services start.
- Logging & volumes: bind logs to host only when necessary; volumes for Postgres/Redis persist data, and pruning steps are documented in `DEPLOYMENT_GUIDE.md`.
- Image publishing: CI builds, tags (`service:gitsha`), scans (Trivy/Grype), and pushes images before any deployment promotion.

## Kubernetes & Runtime Operations
- Deploy each microservice as its own Deployment with liveness/readiness probes mapped to actuator endpoints; Temporal worker uses dedicated worker pods with sticky task queues.
- Define resource requests/limits from load testing baselines; enable HPA on CPU + custom latency metrics for order-service/payment-service.
- ConfigMaps store non-secret YAML, Secrets hold DB/Kafka/JWT values; mount `.env` parity via Downward API, never through baked images.
- Use a service mesh or gateway (nginx/Ingress) for TLS termination and request routing; enforce mTLS between internal services.
- Observability: publish traces/metrics/logs to the cluster stack (OTel collector + Prometheus + Loki); set SLOs per service and wire alerts.
- Release strategy: use rolling updates with max surge 1; protect availability with PodDisruptionBudgets and run blue/green only for risky migrations.
- Data plane dependencies (Postgres, Kafka, Redis, Temporal) run as managed services or dedicated stateful workloads with backup + disaster recovery plans.

## Cross-Cutting & Supporting Practices
### Observability & Incident Response
- Correlate every request with `X-Correlation-ID`; backend propagates it through Kafka events and Temporal contexts, and frontends display it on order timelines.
- Centralize dashboards for API latency, inventory reservations, payment failures, and SSE connection health; alerts include runbooks and auto-ticketing.

### Security & Compliance
- Enforce least privilege roles (CUSTOMER, WAREHOUSE, ADMIN) end-to-end; Flutter/React gates mirror backend checks but backend remains source of truth.
- All secrets managed via Vault/Secrets Manager; rotate JWT signing keys quarterly and require signed commits for releases.
- Run dependency and container scans on every merge; patch CVEs before release unless a documented exception exists.

### CI/CD & Quality Automation
- Single pipeline orchestrates Maven build (`./mvnw -pl !frontend!frontend-web` etc.), frontend builds/tests, Flutter builds/tests, Docker image creation, and deployment manifests.
- Pull requests require: successful pipeline, reviewer sign-off for impacted domain, updated docs (README/guide) when behavior changes, and verified database migrations.
- Use canary deployments with automated smoke tests hitting `/orders`, `/inventory`, `/payments`, and frontend journeys before broad rollout.

### Documentation & Knowledge Sharing
- **Zero-noise output:** Do not generate documentation, diagrams, or verbose explanations unless explicitly requested in the prompt.
- **Concise summaries:** Provide only a precise, bulleted summary of changes at the end of code generation tasks to conserve tokens.
