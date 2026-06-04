package com.nadimnesar.jobqueue.producer.service.impl;

import com.nadimnesar.jobqueue.common.constants.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constants.enums.JobStatus;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import com.nadimnesar.jobqueue.producer.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final JobRepository jobRepository;

    @Override
    public CommonResponse getJobsSummary() {
        Map<String, Long> summary = new HashMap<>();

        for (JobStatus status : JobStatus.values()) {
            long count = jobRepository.countByStatus(status);
            summary.put(status.name(), count);
        }

        long totalJobs = jobRepository.count();
        summary.put("TOTAL", totalJobs);

        return CommonResponse.builder()
                .data(summary)
                .build();
    }

    @Override
    public CommonResponse getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        Map<String, Long> queueMetrics = getQueueMetrics();
        metrics.put("queues", queueMetrics);

        return CommonResponse.builder()
                .data(metrics)
                .build();
    }

    private Map<String, Long> getQueueMetrics() {
        Map<String, Long> queueMetrics = new HashMap<>();
        queueMetrics.put(
                "HIGH_PRIORITY_QUEUE_LENGTH",
                jobRepository.countPendingByPriority(JobPriority.HIGH));

        queueMetrics.put(
                "MEDIUM_PRIORITY_QUEUE_LENGTH",
                jobRepository.countPendingByPriority(JobPriority.MEDIUM));

        queueMetrics.put(
                "LOW_PRIORITY_QUEUE_LENGTH",
                jobRepository.countPendingByPriority(JobPriority.LOW));

        queueMetrics.put(
                "DEAD_LETTER_QUEUE_LENGTH",
                jobRepository.countByStatus(JobStatus.FAILED));

        return queueMetrics;
    }
}
