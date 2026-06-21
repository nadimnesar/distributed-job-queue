package com.nadimnesar.jobqueue.worker.handler;

import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JobHandlerRegistry {
    private final Map<JobType, JobHandler> handlerMap;

    public JobHandlerRegistry(List<JobHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(
                        JobHandler::getType,
                        Function.identity()
                ));
    }

    public JobHandler getHandler(JobType type) {
        JobHandler handler = handlerMap.get(type);

        if (handler == null) {
            throw new IllegalArgumentException("No handler found for type: " + type);
        }
        return handler;
    }
}
