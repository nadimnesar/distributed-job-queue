# Distributed Job Queue

A scalable distributed job queue system built with RabbitMQ and Spring Boot, designed to efficiently distribute
computational tasks across multiple worker nodes with robust fault tolerance, priority-based processing, and DAG-based
dependency management.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Job Lifecycle](#job-lifecycle)
- [Configuration](#configuration)
- [Observability](#observability)
- [Troubleshooting](#troubleshooting)
- [Future Enhancements](#future-enhancements)
- [Contributing](#contributing)
- [License](#license)

## Features

### Core

- Priority-based job processing (HIGH, MEDIUM, LOW) with consumer priority queues
- DAG-based dependency management with unlimited dependencies per job
- Priority hierarchy enforcement — HIGH jobs cannot depend on MEDIUM/LOW jobs
- Job lifecycle tracking with status transitions (PENDING → PROCESSING → COMPLETED/FAILED/DEAD)
- Automatic retry with configurable max attempt count (default: 5, no upper limit)
- Delayed retries via a DELAY queue (30-second TTL) that routes failed jobs to a RETRY queue
- Job cancellation with dependent job awareness
- Dead job revival (single or bulk)

### Infrastructure

- Publisher confirms for reliable message delivery
- ACK/NACK-based message consumption for reliable processing
- Dead Letter Queues (DLQ) for unprocessable jobs with manual recovery support
- Quorum queues with reject-publish overflow strategy to prevent unbounded memory growth
- Virtual threads for high-concurrency job execution
- PgBouncer for efficient connection pooling across producers and workers
- Highly available multi-node RabbitMQ cluster

### Deployment & Observability

- Kubernetes-native deployment with Kustomize overlays
- NGINX Ingress as the external entry point with rate limiting
- Load balancing across multiple producer replicas
- Centralized logging pipeline: Fluent Bit → Elasticsearch → Kibana
- Distributed tracing with traceId/spanId in every API response

## Architecture

![Distributed Job Queue System Design](docs/distributed-job-queue.svg)

```
┌────────────┐      POST /api/v1/job/create      ┌──────────────┐
│  Producer   │ ──────────────────────────────────▶│  PostgreSQL   │
│  (this API) │                                    │  (jobs table)  │
└──────┬─────┘                                    └──────┬───────┘
       │                                                 │
       │  afterCommit()                                  │
       ▼                                                 ▼
┌──────────────┐   consume / publish   ┌──────────────────────┐
│   RabbitMQ    │◀────────────────────▶│     Worker Service    │
│  (quorum      │                      │  (separate deployable)│
│   queues)     │                      └──────────────────────┘
└──────────────┘
```

- **Producer**: Accepts job submissions via REST API, validates dependencies via DAG check, persists jobs to PostgreSQL,
  and publishes to RabbitMQ only after the database transaction commits.
- **Workers**: Independent deployables that consume from priority queues, process jobs, and acknowledge or reject
  messages.
- **RabbitMQ**: Six quorum queues — HIGH, MEDIUM, LOW priority queues, a DELAY queue (30-second TTL for delayed retries), a RETRY queue, and a DLQ (Dead Letter Queue).
- **PostgreSQL**: Source of truth for job state. Workers update status transitions in the database.

## Tech Stack

| Layer              | Technology                          |
|--------------------|-------------------------------------|
| Runtime            | Java 25, Spring Boot 4.0.6          |
| Persistence        | PostgreSQL (via Spring Data JPA)    |
| Connection Pooling | PgBouncer                           |
| Messaging          | RabbitMQ (quorum queues)            |
| Migration          | Liquibase                           |
| Build              | Maven (multi-module)                |
| Orchestration      | Kubernetes (Minikube for local dev) |
| Ingress            | NGINX Ingress Controller            |
| Logging            | Fluent Bit → Elasticsearch → Kibana |

## Project Structure

```
distributed-job-queue/
├── common/          # Shared entities, repositories, DTOs, and RabbitMQ config
├── producer/        # REST API for job submission, tracking, and dashboard
├── worker/          # Job consumer and processor (separate deployable)
├── migration/       # Liquibase database migrations
├── k8s/             # Kubernetes manifests (base + overlays)
├── docs/            # Architecture SVG and OpenAPI spec
└── Dockerfile       # Multi-stage build for all modules
```

- **common** — Shared library containing entities (`JobEntity`, `JobDependencyEntity`), repositories, enums (
  `JobStatus`, `JobType`, `JobPriority`), RabbitMQ configuration, and DTOs used by both producer and worker.
- **producer** — Spring Boot REST API with endpoints for job creation, cancellation, revival, filtering, and dashboard
  metrics.
- **worker** — Spring Boot service that consumes jobs from RabbitMQ, processes them via registered handlers, and manages
  retry/dead-letter logic.
- **migration** — Standalone Spring Boot app that runs Liquibase migrations on startup.

## Getting Started

### Prerequisites

- Java 25
- Maven 3.9+
- Docker
- kubectl
- Minikube

### Build and Run

1. Clone the repository:
   ```bash
   git clone https://github.com/nadimnesar/distributed-job-queue.git
   cd distributed-job-queue
   ```

2. Start Minikube and enable the ingress addon:
   ```bash
   make start
   ```

3. Build the service images into the Minikube Docker daemon:
   ```bash
   make build
   ```

4. Apply the Kustomize overlay to the cluster:
   ```bash
   make deploy
   ```

5. Delete the resources when done:
   ```bash
   make delete
   ```

### Useful Commands

| Command               | Description                     |
|-----------------------|---------------------------------|
| `make status`         | Show Minikube status            |
| `make ip`             | Print the Minikube IP           |
| `make pod`            | List pods in all namespaces     |
| `make svc`            | List services in all namespaces |
| `make pvc`            | List persistent volume claims   |
| `make logs-producer`  | Tail producer logs              |
| `make logs-worker`    | Tail worker logs                |
| `make logs-rabbitmq`  | Tail RabbitMQ logs              |
| `make logs-postgres`  | Tail PostgreSQL logs            |
| `make logs-fluentbit` | Tail Fluent Bit logs            |
| `make logs-es`        | Tail Elasticsearch logs         |
| `make logs-kibana`    | Tail Kibana logs                |

## API Reference

All responses are wrapped in a `CommonResponse` envelope:

```json
{
  "message": "Human-readable status message",
  "code": 200,
  "data": {
    "...": "..."
  },
  "traceId": "...",
  "spanId": "..."
}
```

On error responses, `data` is always omitted.

Full API documentation is available in [`docs/openapi.yaml`](docs/openapi.yaml).

### Jobs

| Method | Endpoint                   | Description                                     |
|--------|----------------------------|-------------------------------------------------|
| `POST` | `/api/v1/job/create`       | Create a new job                                |
| `GET`  | `/api/v1/jobs`             | List jobs (paginated, optional: `page`, `size`) |
| `GET`  | `/api/v1/jobs/{id}`        | Get job by ID                                   |
| `GET`  | `/api/v1/jobs/filter`      | Filter jobs by status and/or type               |
| `POST` | `/api/v1/jobs/{id}/cancel` | Cancel a job                                    |
| `POST` | `/api/v1/jobs/{id}/revive` | Revive a single dead job                        |
| `POST` | `/api/v1/jobs/revive`      | Revive all dead jobs                            |

### Dashboard

| Method | Endpoint                          | Description                      |
|--------|-----------------------------------|----------------------------------|
| `GET`  | `/api/v1/dashboard/jobs/summary`  | Get job counts grouped by status |
| `GET`  | `/api/v1/dashboard/queue-metrics` | Get queue message counts         |

### Examples

**Create a job:**

```bash
curl -X POST http://192.168.49.2/api/v1/job/create \
  -H "Content-Type: application/json" \
  -d '{
    "priority": "HIGH",
    "type": "EMAIL_SENDING",
    "payload": "Send welcome email to user 123",
    "maxAttemptCount": 3
  }'
```

**Create a job with dependencies:**

```bash
curl -X POST http://192.168.49.2/api/v1/job/create \
  -H "Content-Type: application/json" \
  -d '{
    "priority": "HIGH",
    "type": "PAYMENT_SENDING",
    "payload": "Process refund for order 456",
    "dependencies": [
      "550e8400-e29b-41d4-a716-446655440000",
      "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
    ],
    "maxAttemptCount": 5
  }'
```

**List pending jobs:**

```bash
curl "http://192.168.49.2/api/v1/jobs/filter?status=PENDING&page=0&size=20"
```

**Get dashboard summary:**

```bash
curl http://192.168.49.2/api/v1/dashboard/jobs/summary
```

### Error Handling

| HTTP Status | Cause                                                          |
|-------------|----------------------------------------------------------------|
| `400`       | Validation error, malformed JSON, or business rule violation   |
| `404`       | Job not found or no dead jobs to revive                        |
| `409`       | Conflict (e.g., cancelling a job that is currently PROCESSING) |
| `500`       | Internal server error                                          |

## Job Lifecycle

```
PENDING ──▶ PROCESSING ──▶ COMPLETED   (happy path)
              │
              ├──▶ FAILED ──▶ PENDING   (auto-retry, attemptCount < maxAttemptCount)
              │       │
              │       └──▶ DEAD          (attemptCount >= maxAttemptCount, manual revive required)
              │
              └──▶ CANCELED             (terminal; dependents are informed)
```

- **PENDING** — Job created and waiting in the queue.
- **PROCESSING** — A worker has picked up the job.
- **COMPLETED** — Job finished successfully; `result` may contain output.
- **FAILED** — Job failed but retries remain; automatically re-queued.
- **DEAD** — All retries exhausted; requires manual revive via `/api/v1/jobs/{id}/revive` or `/api/v1/jobs/revive`.
- **CANCELED** — Job was cancelled by the producer; terminal state.

### DAG-Based Dependency Rules

Jobs can declare any number of dependencies (UUIDs of other jobs). The system enforces:

1. **Existence** — All dependency UUIDs must reference existing jobs.
2. **No terminal deps** — A job cannot depend on a CANCELED or DEAD job.
3. **Priority hierarchy** — A HIGH-priority job cannot depend on MEDIUM or LOW. MEDIUM cannot depend on LOW.
4. **DAG integrity** — Dependencies form a directed acyclic graph; cyclic dependencies are rejected.

## Configuration

Key configuration values (set via environment variables or `application.properties`):

| Property                                     | Default                | Description                                                |
|----------------------------------------------|------------------------|------------------------------------------------------------|
| `AppConstants.DEFAULT_MAXIMUM_ATTEMPT_COUNT` | 5                      | Default retry count when `maxAttemptCount` is not provided |
| `maxAttemptCount` (per job) | 5 | Configurable per job, min 1, no upper limit |
| NGINX rate limiting                          | Configured per-ingress | Applied at the NGINX Ingress layer                         |

## Observability

### Logs

Access Kibana at `http://192.168.49.2` to search, visualize, and aggregate logs from all services.

### Tracing

Every API response includes `traceId` and `spanId` fields for distributed tracing. Use these to correlate requests
across producer and worker services.

### Queue Metrics

Monitor queue depths via the dashboard endpoint:

```bash
curl http://192.168.49.2/api/v1/dashboard/queue-metrics
```

Returns counts for: `HIGH_PRIORITY_QUEUE_LENGTH`, `MEDIUM_PRIORITY_QUEUE_LENGTH`, `LOW_PRIORITY_QUEUE_LENGTH`,
`RETRY_QUEUE_LENGTH`, `DEAD_LETTER_QUEUE_LENGTH`.

## Troubleshooting

| Problem                    | Solution                                                                          |
|----------------------------|-----------------------------------------------------------------------------------|
| Pods not starting          | Run `make pod` to check pod status; ensure Minikube is running with `make status` |
| Ingress not reachable      | Verify ingress controller is ready: `kubectl get pods -n ingress-nginx`           |
| Jobs stuck in PENDING      | Check worker logs (`make logs-worker`) and RabbitMQ queue depths                  |
| Jobs going to DEAD         | Check worker logs for error details; use `/api/v1/jobs/revive` to retry           |
| Cannot connect to API      | Ensure Minikube IP is correct (`make ip`) and ingress addon is enabled            |
| Database connection issues | Check PostgreSQL pods and PgBouncer configuration                                 |

## Future Enhancements

- [ ] Improve job distribution algorithm for better worker utilization
- [ ] Implement job scheduling capabilities
- [ ] Add PostgreSQL replication for database redundancy
- [ ] Add advanced monitoring with Prometheus and Grafana

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
