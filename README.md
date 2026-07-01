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
- [Observability](#observability)
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
- Custom maxAttemptCount per job (min 1, no upper limit, default 5)
- Delayed retries via a DELAY queue (30-second TTL) that routes failed jobs to a RETRY queue
- Job cancellation with dependent job awareness
- Dead job revival (single or bulk)
- Job type routing with handler registry and auto-discovery of JobHandler beans
- Stale dependency cleanup during retry (completed and orphan dependency rows automatically removed)

### Infrastructure

- Publisher confirms for reliable message delivery
- ACK/NACK-based message consumption for reliable processing
- Dead Letter Queues (DLQ) for unprocessable jobs with manual recovery support
- Quorum queues with reject-publish overflow strategy to prevent unbounded memory growth
- Virtual threads for high-concurrency job execution
- PgBouncer for efficient connection pooling across producers and workers
- Highly available multi-node RabbitMQ cluster
- KEDA-based autoscaling of worker pods based on queue length (>10 messages, cooldown 60s, polling 15s)
- Graceful shutdown with 55-second timeout for in-flight jobs (ordered shutdown with polling)
- RabbitMQ startup health check with 3 retries (configurable via application properties)
- Time-ordered UUIDs for better database indexing performance

### Deployment & Observability

- Kubernetes-native deployment with Kustomize overlays
- NGINX Ingress as the external entry point with rate limiting
- Load balancing across multiple producer replicas
- Centralized logging pipeline: Fluent Bit → Elasticsearch → Kibana
- Distributed tracing with traceId/spanId in every API response

## Architecture

![Distributed Job Queue System Design](docs/distributed-job-queue.drawio.svg)

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
├── common/          # Shared entities, repositories, DTOs, RabbitMQ config, and tracing utilities
├── producer/        # REST API for job submission, tracking, and dashboard
├── worker/          # Job consumer, processor, and retry handler
├── migration/       # Liquibase database migrations (runs as K8s Job)
├── k8s/             # Kubernetes manifests (base + overlays for dev/prod)
├── docs/            # Architecture SVG and OpenAPI spec
└── Dockerfile       # Multi-stage build for all modules
```

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

2. Start Minikube and enable ingress and keda:
   ```bash
   make setup
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
| `make apply`          | Apply dev overlay to cluster    |
| `make logs-producer`  | Tail producer logs              |
| `make logs-worker`    | Tail worker logs                |
| `make logs-rabbitmq`  | Tail RabbitMQ logs              |
| `make logs-postgres`  | Tail PostgreSQL logs            |
| `make logs-fluentbit` | Tail Fluent Bit logs            |
| `make logs-es`        | Tail Elasticsearch logs         |
| `make logs-kibana`    | Tail Kibana logs                |

## API Reference

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

### DAG-Based Dependency Rules

Jobs can declare any number of dependencies (UUIDs of other jobs). The system enforces:

1. **Existence** — All dependency UUIDs must reference existing jobs.
2. **No terminal deps** — A job cannot depend on a CANCELED or DEAD job.
3. **Priority hierarchy** — A HIGH-priority job cannot depend on MEDIUM or LOW. MEDIUM cannot depend on LOW.
4. **DAG integrity** — Dependencies form a directed acyclic graph; cyclic dependencies are rejected.

## Observability

### Logs

Access Kibana at `http://<minikube-ip>` to search, visualize, and aggregate logs from all services.

### Tracing

Every API response includes `traceId` and `spanId` fields for distributed tracing. Use these to correlate requests
across producer and worker services.

## Future Enhancements

- [ ] Improve job distribution algorithm for better worker utilization
- [ ] Implement job scheduling capabilities
- [ ] Add PostgreSQL replication for database redundancy
- [ ] Add circuit breaker pattern for external service calls

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
