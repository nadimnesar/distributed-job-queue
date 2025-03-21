package com.nadimnesar.jobqueue.common.service;

public interface RedisQueueService {
    void enqueue(String key, String value);

    String dequeue(String key);

    Boolean acquireLock(String lockKey, long expireTime);

    void releaseLock(String lockKey);
}
