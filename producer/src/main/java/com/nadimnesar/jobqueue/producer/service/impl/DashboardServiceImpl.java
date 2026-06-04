package com.nadimnesar.jobqueue.producer.service.impl;

import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import com.nadimnesar.jobqueue.producer.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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

        logger.info("TOTAL JOBS: {}", totalJobs);
        return CommonResponse.builder()
                .data(summary)
                .build();
    }

    @Override
    public CommonResponse getQueueMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        Map<String, Long> queueMetrics = getQueueMetricsData();
        metrics.put("queues", queueMetrics);

        logger.info("QUEUES METRICS: {}", queueMetrics);
        return CommonResponse.builder()
                .data(metrics)
                .build();
    }

    //TODO: Get info from rabbitmq
    private Map<String, Long> getQueueMetricsData() {
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
