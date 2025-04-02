package com.nadimnesar.jobqueue.producer.service.impl;

import com.nadimnesar.jobqueue.common.constant.RedisConstant;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.dto.WorkerHealth;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.RedisQueueService;
import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import com.nadimnesar.jobqueue.producer.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.KeysScanOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final RedissonClient redissonClient;
    private final JobRepository jobRepository;
    private final RedisQueueService redisQueueService;

    @Override
    public CommonResponse getJobsSummary() {
        Map<String, Long> summary = new HashMap<>();

        for (JobStatus status : JobStatus.values()) {
            long count = jobRepository.countByStatus(status);
            summary.put(status.name(), count);
        }

        long totalJobs = jobRepository.count();
        summary.put("TOTAL", totalJobs);

        long deadJobs = redisQueueService.getQueueLength(RedisConstant.DEAD_LETTER_QUEUE_KEY);
        summary.put("DEAD", deadJobs);

        return CommonResponse.builder()
                .data(summary)
                .build();
    }

    @Override
    public CommonResponse getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        Map<String, Long> queueMetrics = getQueueMetrics();
        metrics.put("queues", queueMetrics);

        // Get worker health metrics
        try {
            List<WorkerHealth> workerHealthList = new ArrayList<>();

            Iterable<String> workerHealthKeys = redissonClient.getKeys()
                    .getKeys(KeysScanOptions.defaults()
                            .pattern(RedisConstant.REDIS_WORKER_HEALTH_KEY_PATTERN));

            for (String key : workerHealthKeys) {
                RBucket<WorkerHealth> bucket = redissonClient.getBucket(key);
                WorkerHealth healthData = bucket.get();
                if (healthData != null) {
                    workerHealthList.add(healthData);
                }
            }

            metrics.put("workers", workerHealthList);
            metrics.put("activeWorkers", workerHealthList.size());
        } catch (Exception e) {
            logger.error("Error retrieving worker health metrics, error: {}", e.getMessage(), e);
            metrics.put("workers", Collections.emptyList());
            metrics.put("activeWorkers", 0);
        }

        return CommonResponse.builder()
                .data(metrics)
                .build();
    }

    private Map<String, Long> getQueueMetrics() {
        Map<String, Long> queueMetrics = new HashMap<>();
        queueMetrics.put(
                "HIGH_PRIORITY_QUEUE_LENGTH",
                redisQueueService.getQueueLength(RedisConstant.HIGH_PRIORITY_JOB_QUEUE_KEY));

        queueMetrics.put(
                "MEDIUM_PRIORITY_QUEUE_LENGTH",
                redisQueueService.getQueueLength(RedisConstant.MEDIUM_PRIORITY_JOB_QUEUE_KEY));

        queueMetrics.put(
                "LOW_PRIORITY_QUEUE_LENGTH",
                redisQueueService.getQueueLength(RedisConstant.LOW_PRIORITY_JOB_QUEUE_KEY));

        queueMetrics.put(
                "DEAD_LETTER_QUEUE_LENGTH",
                redisQueueService.getQueueLength(RedisConstant.DEAD_LETTER_QUEUE_KEY));

        return queueMetrics;
    }
}
