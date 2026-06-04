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
- Implement a centralized logging pipeline (Fluent Bit → Logstash → Elasticsearch → Kibana) for observability.
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
* **Logging Pipeline**: Fluent Bit → Logstash → Elasticsearch → Kibana

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

2. Start the services:
   ```bash
   make up
   ```

3. Stop the services:
   ```bash
   make down
   ```

### API Endpoints

Download the Postman collection and environment for testing the APIs.

[Postman Collection](docs/postman/distributed-job-queue.postman_collection.json)
[Postman Environment](docs/postman/distributed-job-queue.postman_environment.json)

### Logs

All logs are stored in the following directory: `docs/logs/`

Use the following commands to view logs in real-time:

```bash
# View log files for all producer instances
ls docs/logs/producer-*.log
# View logs for a specific producer instance (example: producer-062f8e6ef23)
tail -f docs/logs/producer-062f8e6ef23.log
```

## Future Enhancements

- [ ] Improve job distribution algorithm for better worker utilization
- [ ] Implement job scheduling capabilities
- [ ] Add PostgreSQL replication for database redundancy
- [ ] Add advanced monitoring with Prometheus and Grafana

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
