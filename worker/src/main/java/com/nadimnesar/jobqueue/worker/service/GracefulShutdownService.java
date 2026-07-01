package com.nadimnesar.jobqueue.worker.service;

import com.nadimnesar.jobqueue.worker.config.InFlightJobCounter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GracefulShutdownService {

    static final long DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 55;
    private static final long POLL_INTERVAL_MS = 1000;

    private final ExecutorService executor;
    private final InFlightJobCounter inFlightJobCounter;
    private final long shutdownTimeoutSeconds;

    @Autowired
    public GracefulShutdownService(ExecutorService virtualThreadParTaskExecutor,
                                   InFlightJobCounter inFlightJobCounter) {
        this(virtualThreadParTaskExecutor, inFlightJobCounter, DEFAULT_SHUTDOWN_TIMEOUT_SECONDS);
    }

    GracefulShutdownService(ExecutorService virtualThreadParTaskExecutor,
                            InFlightJobCounter inFlightJobCounter,
                            long shutdownTimeoutSeconds) {
        this.executor = virtualThreadParTaskExecutor;
        this.inFlightJobCounter = inFlightJobCounter;
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Graceful shutdown initiated — waiting for in-flight jobs to complete");

        long deadlineMillis = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(shutdownTimeoutSeconds);

        // Wait for in-flight jobs to drain
        while (inFlightJobCounter.get() > 0 && System.currentTimeMillis() < deadlineMillis) {
            long remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(deadlineMillis - System.currentTimeMillis());
            log.info("Waiting for {} in-flight job(s) to complete ({}s remaining)",
                    inFlightJobCounter.get(), remainingSeconds);
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Shutdown wait interrupted — proceeding with executor shutdown");
                break;
            }
        }

        int remaining = inFlightJobCounter.get();
        if (remaining > 0) {
            log.warn("{} in-flight job(s) did not complete within {}s — forcing shutdown",
                    remaining, shutdownTimeoutSeconds);
        } else {
            log.info("All in-flight jobs completed — proceeding with executor shutdown");
        }

        // Initiate executor shutdown (no new tasks accepted)
        executor.shutdown();

        // Wait for running tasks to finish, using remaining time budget
        long remainingMillis = deadlineMillis - System.currentTimeMillis();
        if (remainingMillis > 0) {
            try {
                if (!executor.awaitTermination(remainingMillis, TimeUnit.MILLISECONDS)) {
                    log.warn("Executor did not terminate within remaining {}ms — some tasks may be abandoned",
                            remainingMillis);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Executor awaitTermination interrupted");
            }
        }

        log.info("Graceful shutdown complete");
    }
}
