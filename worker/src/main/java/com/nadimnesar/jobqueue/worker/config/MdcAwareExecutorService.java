package com.nadimnesar.jobqueue.worker.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;

import java.util.*;
import java.util.concurrent.*;

public class MdcAwareExecutorService implements ExecutorService {

    private final ExecutorService delegate;

    public MdcAwareExecutorService(ExecutorService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
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
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(wrapTasks(tasks));
    }

    @Override
    @NonNull
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks,
                                         long timeout,
                                         @NonNull TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(wrapTasks(tasks), timeout, unit);
    }

    @Override
    @NonNull
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return delegate.invokeAny(wrapTasks(tasks));
    }

    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks,
                           long timeout,
                           @NonNull TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(wrapTasks(tasks), timeout, unit);
    }

    private <T> List<Callable<T>> wrapTasks(Collection<? extends Callable<T>> tasks) {
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }
        return tasks.stream()
                .map(this::wrap)
                .toList();
    }

    private static void setMdc(Map<String, String> context) {
        if (context == null || context.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }

    private Runnable wrap(Runnable task) {
        final Map<String, String> captured = MDC.getCopyOfContextMap();

        return () -> {
            final Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                setMdc(captured);
                task.run();
            } finally {
                setMdc(previous);
            }
        };
    }

    private <T> Callable<T> wrap(Callable<T> task) {
        final Map<String, String> captured = MDC.getCopyOfContextMap();

        return () -> {
            final Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                setMdc(captured);
                return task.call();
            } finally {
                setMdc(previous);
            }
        };
    }
}
