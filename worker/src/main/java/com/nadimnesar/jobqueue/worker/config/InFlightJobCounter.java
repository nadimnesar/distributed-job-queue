package com.nadimnesar.jobqueue.worker.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared counter tracking the number of in-flight jobs submitted to the executor.
 * Used by {@link com.nadimnesar.jobqueue.worker.config.AppConfig} to wrap executor submissions
 * and by {@link com.nadimnesar.jobqueue.worker.service.GracefulShutdownService} to wait for
 * completion before shutdown.
 */
@Component
public class InFlightJobCounter {

    private final AtomicInteger counter = new AtomicInteger(0);

    public void increment() {
        counter.incrementAndGet();
    }

    public void decrement() {
        counter.decrementAndGet();
    }

    public int get() {
        return counter.get();
    }
}
