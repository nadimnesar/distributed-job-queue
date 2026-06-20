package com.nadimnesar.jobqueue.producer.interceptor;

import com.nadimnesar.jobqueue.common.constant.Constants;
import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class TracingResponseBodyAdviceTest {

    private static final Class<? extends HttpMessageConverter<?>> DUMMY_CONVERTER_TYPE =
            (Class<? extends HttpMessageConverter<?>>) (Class<?>) HttpMessageConverter.class;

    private TracingResponseBodyAdvice advice;

    @Mock
    private MethodParameter returnType;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @BeforeEach
    void setUp() {
        advice = new TracingResponseBodyAdvice();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void supports_shouldReturnTrue_whenReturnTypeIsCommonResponse() throws NoSuchMethodException {
        MethodParameter param = new MethodParameter(
                TracingResponseBodyAdviceTest.class.getDeclaredMethod("sampleCommonResponseReturn"), -1);
        assertTrue(advice.supports(param, DUMMY_CONVERTER_TYPE));
    }

    @Test
    void supports_shouldReturnTrue_whenReturnTypeIsResponseEntityOfCommonResponse() throws NoSuchMethodException {
        MethodParameter param = new MethodParameter(
                TracingResponseBodyAdviceTest.class.getDeclaredMethod("sampleResponseEntityReturn"), -1);
        assertTrue(advice.supports(param, DUMMY_CONVERTER_TYPE));
    }

    @Test
    void supports_shouldReturnFalse_whenReturnTypeIsResponseEntityOfString() throws NoSuchMethodException {
        MethodParameter param = new MethodParameter(
                TracingResponseBodyAdviceTest.class.getDeclaredMethod("sampleStringEntityReturn"), -1);
        assertFalse(advice.supports(param, DUMMY_CONVERTER_TYPE));
    }

    @Test
    void supports_shouldReturnFalse_whenReturnTypeIsString() throws NoSuchMethodException {
        MethodParameter param = new MethodParameter(
                TracingResponseBodyAdviceTest.class.getDeclaredMethod("sampleStringReturn"), -1);
        assertFalse(advice.supports(param, DUMMY_CONVERTER_TYPE));
    }

    @Test
    void beforeBodyWrite_shouldSetTraceIdAndSpanId_fromMdc() {
        MDC.put(Constants.MDC_TRACE_ID, "test-trace-id");
        MDC.put(Constants.MDC_SPAN_ID, "test-span-id");

        CommonResponse body = CommonResponse.builder().build();
        CommonResponse result = advice.beforeBodyWrite(body, returnType, null, null, request, response);

        assertNotNull(result);
        assertEquals("test-trace-id", result.getTraceId());
        assertEquals("test-span-id", result.getSpanId());
    }

    @Test
    void beforeBodyWrite_shouldSetNullValues_whenMdcIsEmpty() {
        CommonResponse body = CommonResponse.builder().build();
        CommonResponse result = advice.beforeBodyWrite(body, returnType, null, null, request, response);

        assertNotNull(result);
        assertNull(result.getTraceId());
        assertNull(result.getSpanId());
    }

    @Test
    void beforeBodyWrite_shouldReturnNull_whenBodyIsNull() {
        CommonResponse result = advice.beforeBodyWrite(null, returnType, null, null, request, response);
        assertNull(result);
    }

    @Test
    void beforeBodyWrite_shouldPopulateExistingBody() {
        MDC.put(Constants.MDC_TRACE_ID, "trace-abc");
        MDC.put(Constants.MDC_SPAN_ID, "span-xyz");

        CommonResponse body = CommonResponse.builder()
                .message("Custom message")
                .code(201)
                .data("some data")
                .build();

        CommonResponse result = advice.beforeBodyWrite(body, returnType, null, null, request, response);

        assertNotNull(result);
        assertEquals("Custom message", result.getMessage());
        assertEquals(201, result.getCode());
        assertEquals("some data", result.getData());
        assertEquals("trace-abc", result.getTraceId());
        assertEquals("span-xyz", result.getSpanId());
    }

    // Sample return type methods for supports() testing
    @SuppressWarnings("unused")
    private CommonResponse sampleCommonResponseReturn() {
        return null;
    }

    @SuppressWarnings("unused")
    private ResponseEntity<CommonResponse> sampleResponseEntityReturn() {
        return null;
    }

    @SuppressWarnings("unused")
    private ResponseEntity<String> sampleStringEntityReturn() {
        return null;
    }

    @SuppressWarnings("unused")
    private String sampleStringReturn() {
        return null;
    }
}
