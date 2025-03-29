# Distributed Job Queue System with Redis and Spring Boot

## Overview

This project implements a scalable distributed job queue system using Redis and Spring Boot, designed to efficiently
distribute computational tasks across multiple worker nodes. The system provides robust job tracking, fault tolerance,
and handles job dependencies and failures gracefully.

### Requirements

* **Producer:**
    - Enqueue different types of jobs with priorities: High, Medium, and Low.
    - Maintain job statuses: Pending, Processing, Completed, Failed, and Canceled.
    - Support job dependencies to ensure tasks execute in the correct order.
    - Implement job cancellation.
* **Worker:**
    - Deploy multiple worker nodes that can run on different machines.
    - Support horizontal scaling based on queue length.
    - Implement job progress tracking.
    - Detect and handle job failures.
    - Implement an automatic retry mechanism.
    - Utilize a Dead Letter Queue (DLQ) for persistently failed jobs.
    - Provide dashboard APIs for monitoring system metrics.

## System Design

![System Architecture Diagram](https://i.ibb.co/r2Fx63kT/distributed-job-queue-system-with-redis-4.jpg)

### Technical Specifications

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
    "payload": "",
    "maxRetryAttemptCount": 3
}'
```

#### Get Jobs

Request:

```curl
curl --location 'http://localhost:8090/producer/api/v1/job?id=0195e095-670d-7633-bdde-f830a1f09d74' \
--header 'Content-Type: application/json'
```

```curl
curl --location 'http://localhost:8090/producer/api/v1/jobs?page=0&size=10' \
--header 'Content-Type: application/json'
```

Response:

```json
{
  "message": "Operation Successful.",
  "code": 200,
  "data": {
    "id": "0195e095-670d-7633-bdde-f830a1f09d74",
    "priority": "HIGH",
    "status": "COMPLETED",
    "type": "PAYMENT_PROCESSING",
    "dependents": [],
    "dependencies": [],
    "result": "Job completed successfully",
    "errorMessage": null,
    "currentProgress": 100,
    "currentRetryAttemptCount": 1,
    "maxRetryAttemptCount": 3,
    "startedAt": "2025-03-29T12:36:20.005704",
    "completedAt": "2025-03-29T12:37:20.085251"
  }
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

#### Retry Dead Jobs

Request:

```curl
curl --location --request POST 'http://localhost:8090/producer/api/v1/jobs/retry-dead' \
--header 'Content-Type: application/json'
```

Response:

```json
{
  "message": "Successfully retried 1 dead jobs",
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
