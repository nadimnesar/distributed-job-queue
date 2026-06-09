package com.nadimnesar.jobqueue.worker.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * An {@link ExecutorService} decorator that propagates the MDC (Mapped Diagnostic
 * Context) from the submitting thread to the executing thread.
 * <p>
 * This ensures that tracing context ({@code traceId}, {@code spanId}) set via MDC
 * in the caller is available inside the task, even when running on virtual threads
 * or other pooled executors where thread-local state is not automatically inherited.
 */
public class MdcAwareExecutorService implements ExecutorService {

    private final ExecutorService delegate;

    public MdcAwareExecutorService(ExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(@NonNull Runnable command) {
        delegate.execute(wrap(command));
    }

    @Override
    @NonNull
    public Future<?> submit(@NonNull Runnable task) {
        return delegate.submit(wrap(task));
    }

    @Override
    @NonNull
    public <T> Future<T> submit(@NonNull Runnable task, T result) {
        return delegate.submit(wrap(task), result);
    }

    @Override
    @NonNull
    public <T> Future<T> submit(@NonNull Callable<T> task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    @NonNull
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    @NonNull
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasks.stream().map(this::wrap).toList());
    }

    @Override
    @NonNull
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                         long timeout,
                                         @NonNull TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(tasks.stream().map(this::wrap).toList(), timeout, unit);
    }

    @Override
    @NonNull
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks.stream().map(this::wrap).toList());
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                           long timeout,
                           @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks.stream().map(this::wrap).toList(), timeout, unit);
    }

    // ---- Private helpers ----

    private Runnable wrap(Runnable task) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (context != null) {
                MDC.setContextMap(context);
            } else {
                MDC.clear();
            }
            try {
                task.run();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    private <T> Callable<T> wrap(Callable<T> task) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (context != null) {
                MDC.setContextMap(context);
            } else {
                MDC.clear();
            }
            try {
                return task.call();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
