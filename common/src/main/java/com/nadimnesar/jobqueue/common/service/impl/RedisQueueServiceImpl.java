package com.nadimnesar.jobqueue.common.service.impl;

import com.nadimnesar.jobqueue.common.service.RedisQueueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisQueueServiceImpl implements RedisQueueService {
    private static final Logger logger = LoggerFactory.getLogger(RedisQueueServiceImpl.class);

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void enqueue(String key, String value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    @Override
    public String dequeue(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    @Override
    public Boolean acquireLock(String lockKey, long expireTime) {
        logger.info("Acquiring lock with key: {}, for {} milliseconds", lockKey, expireTime);

        var success = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", expireTime, TimeUnit.MILLISECONDS);
        if (success != null && success) {
            logger.info("Lock acquired with key: {}", lockKey);
        } else {
            logger.info("Failed to acquire lock with key: {}", lockKey);
        }

        return Boolean.TRUE.equals(success);
    }

    @Override
    public void releaseLock(String lockKey) {
        logger.info("Releasing lock with key: {}", lockKey);
        redisTemplate.delete(lockKey);
    }
}
