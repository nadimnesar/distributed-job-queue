package com.nadimnesar.jobqueue.worker.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

@Configuration
public class AppConfig {

    /**
     * Creates a virtual-thread-per-task executor wrapped with MDC propagation
     * and in-flight job counter tracking.
     *
     * <p>{@code destroyMethod = ""} because {@code GracefulShutdownService}
     * handles shutdown and awaitTermination in its {@code @PreDestroy} method
     * — it must run BEFORE the executor bean is destroyed.</p>
     */
    @Bean(destroyMethod = "")
    public ExecutorService virtualThreadParTaskExecutor(InFlightJobCounter inFlightJobCounter) {
        ExecutorService raw = Executors.newVirtualThreadPerTaskExecutor();
        ExecutorService mdcAware = new MdcAwareExecutorService(raw);
        return new TrackedExecutorService(mdcAware, inFlightJobCounter);
    }

    /**
     * Wrapper that increments an in-flight counter on submission and decrements
     * it when the submitted task completes (success or failure).
     */
    private record TrackedExecutorService(ExecutorService delegate,
                                          InFlightJobCounter counter) implements ExecutorService {
        @Override
        public void execute(@NonNull Runnable command) {
            counter.increment();
            delegate.execute(() -> {
                try {
                    command.run();
                } finally {
                    counter.decrement();
                }
            });
        }

        @NonNull
        @Override
        public Future<?> submit(@NonNull Runnable task) {
            counter.increment();
            return delegate.submit(() -> {
                try {
                    task.run();
                } finally {
                    counter.decrement();
                }
            });
        }

        @NonNull
        @Override
        public <T> Future<T> submit(@NonNull Runnable task, T result) {
            counter.increment();
            return delegate.submit(() -> {
                try {
                    task.run();
                } finally {
                    counter.decrement();
                }
            }, result);
        }

        @NonNull
        @Override
        public <T> Future<T> submit(@NonNull Callable<T> task) {
            counter.increment();
            return delegate.submit(() -> {
                try {
                    return task.call();
                } finally {
                    counter.decrement();
                }
            });
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @NonNull
        @Override
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
        public boolean awaitTermination(long timeout,
                                        @NonNull TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @NonNull
        @Override
        public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks)
                throws InterruptedException {
            return delegate.invokeAll(tasks);
        }

        @NonNull
        @Override
        public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks,
                                             long timeout, @NonNull TimeUnit unit)
                throws InterruptedException {
            return delegate.invokeAll(tasks, timeout, unit);
        }

        @NonNull
        @Override
        public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            return delegate.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks,
                               long timeout, @NonNull TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(tasks, timeout, unit);
        }
    }
}
