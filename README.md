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
    Decision2 --> |Yes: DeadLetterQueue| Redis
    Decision2 --> |No| Redis
    
    classDef producer fill:#8E44AD,stroke:#6C3483,color:white,stroke-width:2px,font-weight:bold
    classDef worker fill:#F57C00,stroke:#E65100,color:white,stroke-width:2px,font-weight:bold
    classDef database fill:#336791,stroke:#274472,color:white,stroke-width:2px,font-weight:bold,stroke-dasharray:5 2
    classDef cache fill:#D32F2F,stroke:#B71C1C,color:white,stroke-width:2px,font-weight:bold,stroke-dasharray:5 2
    classDef loadbalancer fill:#388E3C,stroke:#2E7D32,color:white,stroke-width:2px,font-weight:bold
 
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

### Monitoring and Troubleshooting

#### Logs

- Producer service logs: `docker logs -f distributed-job-queue-with-redis-and-spring_producer_1`
- Worker service logs: `docker logs -f  distributed-job-queue-with-redis-and-spring_worker_1`

## API Endpoints

#### Create a Job

Request:

```curl
curl --location 'http://localhost:8090/producer/api/v1/job/create' \
--header 'Content-Type: application/json' \
--data '{
    "priority": "HIGH",
    "type": "PAYMENT_PROCESSING",
    "dependencies": [],
    "payload": "test",
    "maxAttemptCount": 3
}'
```

#### Get Jobs with Pagination

Request:

```curl
curl --location 'http://localhost:8090/producer/api/v1/jobs?page=0&size=10' \
--header 'Content-Type: application/json'
```

Response:

```json
{
  "message": "Operation Successful.",
  "code": 200,
  "data": [
    {
      "id": "0195e719-9c73-7e08-bdf3-a3d01433c344",
      "priority": "HIGH",
      "status": "COMPLETED",
      "type": "PAYMENT_PROCESSING",
      "dependents": [],
      "dependencies": [],
      "result": "Job completed successfully",
      "errorMessage": null,
      "currentProgress": 100,
      "currentAttemptCount": 1,
      "maxAttemptCount": 3,
      "startedAt": "2025-03-30T18:49:40.064995",
      "completedAt": "2025-03-30T18:50:40.183834"
    }
  ]
}
```

#### Get Job Summary

Request:

```curl
curl --location 'http://localhost:8090/producer/api/v1/dashboard/jobs/summary' \
--header 'Content-Type: application/json'
```

Response:

```json
{
  "message": "Operation Successful.",
  "code": 200,
  "data": {
    "TOTAL": 15,
    "COMPLETED": 14,
    "FAILED": 0,
    "PROCESSING": 0,
    "CANCELED": 1,
    "PENDING": 0,
    "DEAD": 0
  }
}
```

#### Get System Metrics

Request:

```curl
curl --location 'http://localhost:8090/producer/api/v1/dashboard/metrics' \
--header 'Content-Type: application/json'
```

Response:

```json
{
  "message": "Operation Successful.",
  "code": 200,
  "data": {
    "activeWorkers": 1,
    "queues": {
      "LOW_PRIORITY_QUEUE_LENGTH": 0,
      "DEAD_LETTER_QUEUE_LENGTH": 0,
      "HIGH_PRIORITY_QUEUE_LENGTH": 0,
      "MEDIUM_PRIORITY_QUEUE_LENGTH": 0
    },
    "workers": [
      {
        "workerId": "0195e0b7-1113-70f4-beaa-85dbf9cfaad7",
        "cpuLoad": 0.7734375,
        "memoryUsagePercentage": 74.73899267053092,
        "availableProcessors": 4,
        "heapMemoryUsage": 71285944,
        "maxHeapMemory": 2025848832,
        "timestamp": 1743233510001
      }
    ]
  }
}
```

#### Revive Dead Jobs

Request:

```curl
curl --location --request POST 'http://localhost:8090/producer/api/v1/jobs/revive' \
--header 'Content-Type: application/json'
```

Response:

```json
{
  "message": "Successfully revived 1 dead jobs",
  "code": 200,
  "data": [
    "0195e095-670d-7633-bdde-f830a1f09d74"
  ]
}
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
