package com.nadimnesar.jobqueue.worker.config;

import com.nadimnesar.jobqueue.common.constant.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MdcAwareExecutorServiceTest {

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = new MdcAwareExecutorService(Executors.newSingleThreadExecutor());
        MDC.clear();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        MDC.clear();
        executorService.shutdownNow();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    void execute_shouldPropagateMdcToTask() throws Exception {
        // Given: MDC is set on the calling thread
        MDC.put(Constants.MDC_TRACE_ID, "test-trace-id");
        MDC.put(Constants.MDC_SPAN_ID, "test-span-id");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        AtomicReference<String> capturedSpanId = new AtomicReference<>();

        // When: executing a task
        executorService.execute(() -> {
            capturedTraceId.set(MDC.get(Constants.MDC_TRACE_ID));
            capturedSpanId.set(MDC.get(Constants.MDC_SPAN_ID));
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        // Then: MDC should be available inside the task
        assertEquals("test-trace-id", capturedTraceId.get());
        assertEquals("test-span-id", capturedSpanId.get());
    }

    @Test
    void submit_shouldPropagateMdcToTask() throws Exception {
        // Given: MDC is set on the calling thread
        MDC.put(Constants.MDC_TRACE_ID, "trace-in-parent");
        MDC.put(Constants.MDC_SPAN_ID, "span-in-parent");

        // When: submitting a task
        var future = executorService.submit(() -> MDC.get(Constants.MDC_TRACE_ID) + ":" + MDC.get(Constants.MDC_SPAN_ID));

        String result = future.get(2, TimeUnit.SECONDS);

        // Then: MDC should be available inside the task
        assertEquals("trace-in-parent:span-in-parent", result);
    }

    @Test
    void execute_shouldNotLeakMdcToSubsequentTasks() throws Exception {
        // Given: First task with MDC, second task without
        MDC.put(Constants.MDC_TRACE_ID, "first-trace");
        MDC.put(Constants.MDC_SPAN_ID, "first-span");

        CountDownLatch firstLatch = new CountDownLatch(1);
        executorService.execute(firstLatch::countDown);
        assertTrue(firstLatch.await(2, TimeUnit.SECONDS));

        // Clear MDC before second task
        MDC.clear();

        CountDownLatch secondLatch = new CountDownLatch(1);
        AtomicReference<String> secondTraceId = new AtomicReference<>();
        executorService.execute(() -> {
            secondTraceId.set(MDC.get(Constants.MDC_TRACE_ID));
            secondLatch.countDown();
        });
        assertTrue(secondLatch.await(2, TimeUnit.SECONDS));

        // Then: second task should not see first task's MDC
        assertNull(secondTraceId.get());
    }

    @Test
    void execute_shouldRestorePreviousMdcAfterTask() throws Exception {
        // Given: MDC is set before task
        MDC.put(Constants.MDC_TRACE_ID, "parent-trace");
        MDC.put(Constants.MDC_SPAN_ID, "parent-span");

        CountDownLatch latch = new CountDownLatch(1);
        executorService.execute(() -> {
            // Modify MDC inside task
            MDC.put(Constants.MDC_TRACE_ID, "modified-inside");
            latch.countDown();
        });
        assertTrue(latch.await(2, TimeUnit.SECONDS));

        // Then: calling thread's MDC should be unaffected
        assertEquals("parent-trace", MDC.get(Constants.MDC_TRACE_ID));
    }

    @Test
    void submitCallable_shouldPropagateMdc() throws Exception {
        MDC.put(Constants.MDC_TRACE_ID, "callable-trace");

        var future = executorService.submit(() -> {
            String tid = MDC.get(Constants.MDC_TRACE_ID);
            MDC.put(Constants.MDC_TRACE_ID, "modified-in-callable");
            MDC.clear();
            return tid;
        });

        String result = future.get(2, TimeUnit.SECONDS);

        // Callable should see parent MDC
        assertEquals("callable-trace", result);
    }

    @Test
    void execute_shouldHandleNullMdcGracefully() throws Exception {
        // Given: No MDC set
        CountDownLatch latch = new CountDownLatch(1);

        // When: executing a task without any MDC context
        executorService.execute(() -> {
            assertNull(MDC.get(Constants.MDC_TRACE_ID));
            assertNull(MDC.get(Constants.MDC_SPAN_ID));
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void threadPoolExecutor_shouldWorkAsDelegate() throws Exception {
        // Test with a fixed thread pool instead of single-thread
        ExecutorService threadPool = new MdcAwareExecutorService(Executors.newFixedThreadPool(2));
        try {
            MDC.put(Constants.MDC_TRACE_ID, "pool-trace");

            CountDownLatch latch = new CountDownLatch(3);
            for (int i = 0; i < 3; i++) {
                threadPool.execute(() -> {
                    String tid = MDC.get(Constants.MDC_TRACE_ID);
                    assertNotNull(tid);
                    latch.countDown();
                });
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } finally {
            threadPool.shutdownNow();
        }
    }
}
