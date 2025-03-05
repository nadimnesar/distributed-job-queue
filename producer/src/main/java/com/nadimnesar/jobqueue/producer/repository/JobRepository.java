package com.nadimnesar.jobqueue.producer.repository;

import com.nadimnesar.jobqueue.producer.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, Long> {
}
