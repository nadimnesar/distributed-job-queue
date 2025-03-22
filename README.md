# Distributed Job Queue System with Redis

## Problem Statement

Design and implement a distributed job queue system using Redis that efficiently distributes computational tasks across
multiple worker nodes. The system should ensure job status tracking, fault tolerance, and scalability while handling job
dependencies and failures gracefully.

### Requirements

* **Job Producer:** Enqueue different types of jobs with varying priorities.
* **Worker Nodes:**
    - Deploy multiple worker nodes that can run on different machines.
    - Support horizontal scaling based on queue length.
* **Job Execution & Tracking:**
    - Maintain job statuses: Pending, Processing, Completed, Failed.
    - Implement job progress tracking.
* **Failure Handling & Retry Mechanism:**
    - Detect and handle job failures.
    - Implement an automatic retry mechanism.
    - Utilize a Dead Letter Queue (DLQ) for persistent failed jobs.
* **Job Dependencies & Control:**
    - Support job dependencies, ensuring tasks execute in the correct order.
    - Implement job cancellation.
* **Result Storage & Retrieval:**
    - Store and retrieve job results efficiently.
    - Provide a dashboard to monitor job statuses and worker health.

### Technical Specifications

* **Backend**: Spring Boot
* **Queue:** Redis
* **Load Balancer:** Nginx
* **Database:** Postgres
* **Database Migration:** Liquibase

## System Design

<img src="https://i.ibb.co.com/r2Fx63kT/distributed-job-queue-system-with-redis-4.jpg" alt="diagram">

## Known Issues

* The current system design does not support horizontal scaling based on queue length. However, scaling is possible by
  modifying the Docker Compose file.
* The current implementation does not support dependency management.
* PostgreSQL and Redis are single points of failure. The solution is to use clustering.
* The job is not efficiently distributed across multiple worker nodes. Currently, whichever worker node consumes the job
  first processes it.
