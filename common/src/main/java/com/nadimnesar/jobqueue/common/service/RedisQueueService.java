package com.nadimnesar.jobqueue.common.service;

import java.util.List;

public interface RedisQueueService {
    void enqueue(String key, String value);

    String dequeue(String key);

    List<String> dequeueAll(String key);

    Boolean acquireLock(String lockKey, long expireTime);

    void releaseLock(String lockKey);

    Long getQueueLength(String key);
}
