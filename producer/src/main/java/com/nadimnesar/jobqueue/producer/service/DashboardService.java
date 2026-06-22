package com.nadimnesar.jobqueue.producer.service;

import com.nadimnesar.jobqueue.common.constant.RabbitMQConstants;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.repository.projection.JobStatusCountProjection;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JobRepository jobRepository;
    private final JobQueueService jobQueueService;

    public CommonResponse getJobsSummary() {
        Map<String, Long> summary = new HashMap<>();

        List<JobStatusCountProjection> results = jobRepository.countJobsGroupedByStatus();
        for (JobStatusCountProjection row : results) {
            summary.put(row.getStatus().name(), row.getCount());
        }

        long totalJobs = jobRepository.count();
        summary.put("TOTAL", totalJobs);

        log.info("TOTAL JOBS: {}", totalJobs);
        return CommonResponse.builder()
                .data(summary)
                .build();
    }

    public CommonResponse getQueueMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        Map<String, Long> queueMetrics = getQueueMetricsData();
        metrics.put("queues", queueMetrics);

        log.info("QUEUES METRICS: {}", queueMetrics);
        return CommonResponse.builder()
                .data(metrics)
                .build();
    }

    private Map<String, Long> getQueueMetricsData() {
        Map<String, Long> queueMetrics = new HashMap<>();
        queueMetrics.put("HIGH_PRIORITY_QUEUE_LENGTH",
                jobQueueService.getQueueMessageCount(RabbitMQConstants.QUEUE_HIGH));
        queueMetrics.put("MEDIUM_PRIORITY_QUEUE_LENGTH",
                jobQueueService.getQueueMessageCount(RabbitMQConstants.QUEUE_MEDIUM));
        queueMetrics.put("LOW_PRIORITY_QUEUE_LENGTH",
                jobQueueService.getQueueMessageCount(RabbitMQConstants.QUEUE_LOW));
        queueMetrics.put("RETRY_QUEUE_LENGTH",
                jobQueueService.getQueueMessageCount(RabbitMQConstants.QUEUE_RETRY));
        queueMetrics.put("DEAD_LETTER_QUEUE_LENGTH",
                jobQueueService.getQueueMessageCount(RabbitMQConstants.QUEUE_DLQ));
        return queueMetrics;
    }
}
