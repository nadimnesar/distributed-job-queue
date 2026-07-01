package com.nadimnesar.jobqueue.worker.service;

import com.nadimnesar.jobqueue.worker.config.InFlightJobCounter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class GracefulShutdownServiceTest {

    private ExecutorService executor;
    private InFlightJobCounter counter;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        counter = new InFlightJobCounter();
    }

    @AfterEach
    void tearDown() {
        if (!executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("AC3: submitting jobs, triggering shutdown, verifying all complete before method returns")
    void shutdown_waitsForInFlightJobsToComplete() throws Exception {
        // Given: multiple jobs are submitted and running
        int jobCount = 3;
        CountDownLatch allStarted = new CountDownLatch(jobCount);
        CountDownLatch releaseLatch = new CountDownLatch(1);
        List<AtomicBoolean> completed = new ArrayList<>();

        GracefulShutdownService shutdownService = new GracefulShutdownService(executor, counter);

        for (int i = 0; i < jobCount; i++) {
            AtomicBoolean done = new AtomicBoolean(false);
            completed.add(done);
            counter.increment();
            executor.submit(() -> {
                try {
                    allStarted.countDown();
                    releaseLatch.await(); // Block until released
                    done.set(true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    counter.decrement();
                }
            });
        }

        // Wait for all jobs to start
        assertTrue(allStarted.await(5, TimeUnit.SECONDS), "All jobs should start within 5s");

        // When: shutdown is triggered in a separate thread
        Thread shutdownThread = new Thread(shutdownService::shutdown);
        shutdownThread.start();

        // Give shutdown a moment to detect in-flight jobs
        Thread.sleep(500);

        // Then: jobs are still running (counter > 0)
        assertTrue(counter.get() > 0, "Jobs should still be in-flight");

        // Release the jobs
        releaseLatch.countDown();

        // Shutdown should complete after jobs finish
        shutdownThread.join(10_000);
        assertFalse(shutdownThread.isAlive(), "Shutdown thread should have completed");

        // And all jobs completed successfully
        for (AtomicBoolean done : completed) {
            assertTrue(done.get(), "Each job should have completed before shutdown returned");
        }

        // And executor is shut down
        assertTrue(executor.isShutdown(), "Executor should be shut down after graceful shutdown");
    }

    @Test
    @DisplayName("AC3: shutdown proceeds immediately when no jobs in-flight")
    void shutdown_proceedsImmediatelyWhenNoJobs() {
        // Given: no jobs in-flight
        assertEquals(0, counter.get());

        GracefulShutdownService shutdownService = new GracefulShutdownService(executor, counter);

        // When
        long start = System.currentTimeMillis();
        shutdownService.shutdown();
        long elapsed = System.currentTimeMillis() - start;

        // Then: shutdown completes quickly
        assertTrue(elapsed < 2000, "Shutdown should complete quickly when no jobs in-flight, took " + elapsed + "ms");
        assertTrue(executor.isShutdown(), "Executor should be shut down");
    }

    @Test
    @DisplayName("AC3: shutdown terminates even when jobs exceed timeout")
    void shutdown_terminatesWhenJobsExceedTimeout() throws Exception {
        // Given: a job that will never complete (blocked indefinitely)
        // Use a short timeout (2s) so the test runs quickly
        long shortTimeoutSeconds = 2;
        GracefulShutdownService shutdownService = new GracefulShutdownService(executor, counter, shortTimeoutSeconds);

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch neverRelease = new CountDownLatch(1);
        executor.submit(() -> {
            started.countDown();
            try {
                neverRelease.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        counter.increment();

        assertTrue(started.await(5, TimeUnit.SECONDS), "Job should start");

        // When: shutdown is triggered (will timeout at 2s)
        long start = System.currentTimeMillis();
        shutdownService.shutdown();
        long elapsed = System.currentTimeMillis() - start;

        // Then: shutdown completes even though job didn't finish
        assertTrue(executor.isShutdown(), "Executor should be shut down even with stuck job");
        // Should complete in roughly 2-3 seconds (timeout + poll interval)
        assertTrue(elapsed < 5000, "Shutdown should complete within timeout, took " + elapsed + "ms");
    }

    @Test
    @DisplayName("AC3: counter decrements correctly after jobs complete")
    void counterTracksJobsCorrectly() throws Exception {
        // Given: submit several quick jobs
        int jobCount = 10;
        for (int i = 0; i < jobCount; i++) {
            counter.increment();
            executor.submit(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    counter.decrement();
                }
            });
        }

        // Wait for all jobs to complete
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        // Then: counter should be back to 0
        assertEquals(0, counter.get(), "Counter should be 0 after all jobs complete");
    }
}
