# Distributed Job Queue

## Overview

This project implements a scalable distributed job queue system using RabbitMQ and Spring Boot, designed to efficiently
distribute computational tasks across multiple worker nodes. The system provides robust job tracking, fault
tolerance,and handles job dependencies and failures gracefully.

### Features

- Deploy multiple worker and producer nodes across multiple machines using Kubernetes.
- Use NGINX Ingress as the external entry point for all inbound traffic; no direct service exposure.
- Apply rate limiting on producer requests via NGINX Ingress.
- Load balance traffic across multiple producer replicas.
- Enqueue jobs with priority support for efficient task processing.
- Ensure reliable message delivery using publisher confirms.
- Use Directed Acyclic Graph (DAG)-based dependency management to guarantee correct execution order.
- Track job statuses and support job cancellation.
- Enable ACK/NACK-based message consumption for reliable processing.
- Support virtual threads for high-concurrency job execution.
- Implement delayed retries with exponential backoff using a queue-based delay mechanism.
- Automatically scale worker replicas based on RabbitMQ queue length.
- Provide fault tolerance using Dead Letter Queues (DLQ) for unprocessable jobs, with support for job recovery.
- Integrate PgBouncer for efficient connection pooling across multiple producers and workers.
- Maintain a highly available multi-node RabbitMQ cluster.
- Implement a centralized logging pipeline (Fluent Bit → Elasticsearch → Kibana) for observability.
- Provide log aggregation, search, and visualization using Elasticsearch and Kibana.

## System Design

![Distributed Job Queue System Design](docs/distributed-job-queue.svg)

## Technical Specifications

* **Backend**: Spring Boot 4.0.6
* **Runtime**: Java 25
* **Message Queue**: RabbitMQ
* **Database**: PostgreSQL
* **Connection Pooling**: PgBouncer
* **Database Migration**: Liquibase
* **Build Tool**: Maven
* **Orchestration**: Kubernetes
* **Logging Pipeline**: Fluent Bit → Elasticsearch → Kibana

## Getting Started

### Prerequisites

* Java 25
* Maven 3.9+
* Docker
* kubectl
* Minikube

### Build and Run the Project

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

Useful commands:

- `make status` — show Minikube status
- `make ip` — print the Minikube IP

### API Endpoints

- `POST /api/v1/job/create` - Create a new job
- `GET /api/v1/jobs` - Get all jobs (paginated)
- `GET /api/v1/jobs/{id}` - Get job by ID
- `GET /api/v1/jobs/filter` - Filter jobs by status and/or type
- `POST /api/v1/jobs/{id}/cancel` - Cancel a job
- `POST /api/v1/jobs/{id}/revive` - Revive a specific dead job
- `POST /api/v1/jobs/revive` - Revive all dead jobs
- `GET /api/v1/dashboard/jobs/summary` - Get jobs summary
- `GET /api/v1/dashboard/queue-metrics` - Get queue metrics

### Logs

Use Kibana: http://192.168.49.2

## Future Enhancements

- [ ] Improve job distribution algorithm for better worker utilization
- [ ] Implement job scheduling capabilities
- [ ] Add PostgreSQL replication for database redundancy
- [ ] Add advanced monitoring with Prometheus and Grafana

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
