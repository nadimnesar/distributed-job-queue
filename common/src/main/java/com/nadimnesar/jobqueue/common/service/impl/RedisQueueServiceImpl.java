package com.nadimnesar.jobqueue.common.service.impl;

import com.nadimnesar.jobqueue.common.constant.RedisConstant;
import com.nadimnesar.jobqueue.common.service.RedisQueueService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisQueueServiceImpl implements RedisQueueService {
    private static final Logger logger = LoggerFactory.getLogger(RedisQueueServiceImpl.class);

    private final RedissonClient redissonClient;

    @Override
    public void enqueue(String key, String value) {
        RQueue<String> queue = redissonClient.getQueue(key);
        queue.add(value);
    }

    @Override
    public String dequeue(String key) {
        RQueue<String> queue = redissonClient.getQueue(key);
        return queue.poll();
    }

    @Override
    public List<String> dequeueAll(String key) {
        RQueue<String> queue = redissonClient.getQueue(key);
        List<String> values = queue.readAll();
        queue.clear();
        return values != null ? values : Collections.emptyList();
    }

    @Override
    public boolean acquireLock(String lockKey) {
        logger.info("Acquiring lock with key: {} with watchdog mode enabled.", lockKey);

        RLock lock = redissonClient.getFairLock(RedisConstant.REDIS_LOCK_PREFIX + lockKey);
        try {
            // Using lock() enables watchdog mode
            // The lock will be held until explicitly released or the process dies
            lock.lock();

            logger.info("Lock acquired successfully with key: {}", lockKey);
            return true;
        } catch (Exception e) {
            logger.error("Error acquiring lock with key {}: {}", lockKey, e.getMessage());
            return false;
        }
    }

    @Override
    public void releaseLock(String lockKey) {
        logger.info("Attempting to release lock with key: {}", lockKey);
        RLock lock = redissonClient.getFairLock(RedisConstant.REDIS_LOCK_PREFIX + lockKey);

        try {
            // Atomic check to ensure only the original owner can release the lock
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                logger.info("Lock released successfully with key: {}", lockKey);
            } else {
                logger.warn("Lock with key: {} is not held by the current thread, skipping unlock", lockKey);
            }
        } catch (Exception e) {
            logger.error("Error releasing lock with key {}: {}", lockKey, e.getMessage(), e);
        }
    }

    @Override
    public Long getQueueLength(String key) {
        RQueue<String> queue = redissonClient.getQueue(key);
        return (long) queue.size();
    }
}
