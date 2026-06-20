package com.nadimnesar.jobqueue.producer.interceptor;

import com.nadimnesar.jobqueue.common.constant.Constants;
import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@ControllerAdvice
public class TracingResponseBodyAdvice implements ResponseBodyAdvice<CommonResponse> {

    @Override
    public boolean supports(MethodParameter returnType,
                            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> type = returnType.getParameterType();

        // Direct CommonResponse return (non-ResponseEntity)
        if (CommonResponse.class.isAssignableFrom(type)) {
            return true;
        }

        // ResponseEntity<CommonResponse> or HttpEntity<CommonResponse>
        if (HttpEntity.class.isAssignableFrom(type)) {
            Type genericType = returnType.getGenericParameterType();
            if (genericType instanceof ParameterizedType parameterizedType) {
                Type[] typeArgs = parameterizedType.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> clazz) {
                    return CommonResponse.class.isAssignableFrom(clazz);
                }
            }
        }

        return false;
    }

    @Override
    public CommonResponse beforeBodyWrite(CommonResponse body,
                                          @NonNull MethodParameter returnType,
                                          @NonNull MediaType selectedContentType,
                                          @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                          @NonNull ServerHttpRequest request,
                                          @NonNull ServerHttpResponse response) {
        if (body != null) {
            body.setTraceId(MDC.get(Constants.MDC_TRACE_ID));
            body.setSpanId(MDC.get(Constants.MDC_SPAN_ID));
        }

        return body;
    }
}
