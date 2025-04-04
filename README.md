# Distributed Job Queue System with Redis and Spring Boot

## Overview

This project implements a scalable distributed job queue system using Redis and Spring Boot, designed to efficiently
distribute computational tasks across multiple worker nodes. The system provides robust job tracking, fault tolerance,
and handles job dependencies and failures gracefully.

### Requirements

* **Producer:**
    - Enqueue jobs with priorities and dependencies.
    - Track job statuses: Pending, Processing, Completed, Failed, and Canceled.
    - Support job cancellation and provide an API to revive dead jobs.
* **Worker:**
    - Deploy multiple worker nodes across different machines.
    - Dynamically scale based on queue length.
    - Track job progress and handle failures.
    - Use a Dead Letter Queue (DLQ) for unprocessable jobs.
    - Implement an automatic retry mechanism.
    - Provide monitoring APIs for worker system metrics.

## System Design

```mermaid
flowchart TD
    User((User)) --> Frontend[Frontend UI]
    Frontend --> Nginx[Load Balancer: Nginx]

    subgraph Producers[Producer Service Cluster]
        direction TB
        Producer1[Producer 1]
        Producer2[Producer 2]
        Producer3[Producer 3]
    end

    Nginx --> Producers
    Producers -->|Enqueue Job| Redis[("Redis: Job Queue")]

    subgraph Workers[Worker Cluster]
        direction TB
        Worker1[Worker 1]
        Worker2[Worker 2]
        Worker3[Worker 3]
        Worker4[Worker 4]
        Worker5[Worker 5]
    end

    Redis -->|Consume Job| Workers
    Workers -->|Update Job| PostgreSQL[("PostgreSQL: Job Metadata")]
    Producers -->|Store Job Metadata| PostgreSQL
    Workers --> Decision1{Successful?}
    Decision1 -->|Yes| Done([Done])
    Decision1 -->|No| Decision2{Limit Exceeded?}
    Decision2 -->|Yes: DeadLetterQueue| Redis
    Decision2 -->|No| Redis
    classDef producer fill: #8E44AD, stroke: #6C3483, color: white, stroke-width: 2px, font-weight: bold
    classDef worker fill: #F57C00, stroke: #E65100, color: white, stroke-width: 2px, font-weight: bold
    classDef database fill: #336791, stroke: #274472, color: white, stroke-width: 2px, font-weight: bold, stroke-dasharray: 5 2
    classDef cache fill: #D32F2F, stroke: #B71C1C, color: white, stroke-width: 2px, font-weight: bold, stroke-dasharray: 5 2
    classDef loadbalancer fill: #388E3C, stroke: #2E7D32, color: white, stroke-width: 2px, font-weight: bold
class Producer1,Producer2,Producer3 producer
class Worker1,Worker2,Worker3,Worker4,Worker5 worker
class PostgreSQL database
class Redis cache
class Nginx loadbalancer
```

## Technical Specifications

* **Backend**: Spring Boot
* **Queue:** Redis
* **Load Balancer:** Nginx
* **Database:** PostgreSQL
* **Database Migration:** Liquibase

## Getting Started

### Prerequisites

* Java 21
* Maven
* Docker & Docker Compose

### Build and Run the Project

1. Clone the repository:
   ```bash
   git clone https://github.com/nadimnesar/distributed-job-queue-with-redis-and-spring.git
   cd distributed-job-queue-with-redis-and-spring
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
# View logs for a specific producer instance
tail -f docs/logs/producer-local.log
```

## Future Enhancements

- [ ] Implement horizontal scaling for worker nodes based on queue length
- [ ] Improve job distribution algorithm for better worker utilization
- [ ] Implement job scheduling capabilities
- [ ] Implement Redis Cluster for high availability
- [ ] Add PostgreSQL replication for database redundancy
- [ ] Add advanced monitoring with Prometheus and Grafana

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
