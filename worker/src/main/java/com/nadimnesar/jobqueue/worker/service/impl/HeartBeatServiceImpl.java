package com.nadimnesar.jobqueue.worker.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import com.nadimnesar.jobqueue.common.constant.RedisConstant;
import com.nadimnesar.jobqueue.common.dto.WorkerHealth;
import com.nadimnesar.jobqueue.worker.service.HeartBeatService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class HeartBeatServiceImpl implements HeartBeatService {
    private static final Logger logger = LoggerFactory.getLogger(HeartBeatServiceImpl.class);

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private UUID workerId;

    @Override
    @Scheduled(cron = "${schedule.cron.heartbeat}")
    public void sendHeartbeat() {
        if (workerId == null) {
            logger.warn("Worker ID is not initialized. Skipping heartbeat.");
            return;
        }

        logger.debug("Sending heartbeat for worker: {}, at: {}", workerId, LocalDateTime.now());
        try {
            WorkerHealth health = collectHealthMetrics();
            String healthJson = objectMapper.writeValueAsString(health);

            String key = RedisConstant.REDIS_WORKER_HEALTH_KEY_PREFIX + workerId + RedisConstant.REDIS_WORKER_HEALTH_KEY_SUFFIX;
            redisTemplate.opsForValue().set(key, healthJson, 10, TimeUnit.SECONDS);

            logger.debug("Heartbeat sent for worker: {}", workerId);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize worker health metrics, error: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Failed to send heartbeat, error: {}", e.getMessage(), e);
        }
    }

    private WorkerHealth collectHealthMetrics() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        double cpuLoad = osBean.getSystemLoadAverage();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        double memoryUsage = (double) (totalMemory - freeMemory) / totalMemory;

        WorkerHealth health = new WorkerHealth();
        health.setWorkerId(this.workerId);
        health.setCpuLoad(cpuLoad);
        health.setMemoryUsagePercentage(memoryUsage * 100);
        health.setAvailableProcessors(osBean.getAvailableProcessors());
        health.setHeapMemoryUsage(memoryBean.getHeapMemoryUsage().getUsed());
        health.setMaxHeapMemory(memoryBean.getHeapMemoryUsage().getMax());
        health.setTimestamp(System.currentTimeMillis());

        return health;
    }

    @PostConstruct
    private void initialize() {
        this.workerId = UuidCreator.getTimeOrderedEpoch();
        logger.debug("Worker initialized with ID: {}", workerId);
    }
}
