package com.nadimnesar.jobqueue.worker.handler;

import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JobHandlerRegistry {
    private final Map<JobType, JobHandler> handlerMap;

    public JobHandlerRegistry(List<JobHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(
                        JobHandler::getType,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException("Duplicate handler for type: " + a.getType()
                                    + " — " + a.getClass().getName() + " and " + b.getClass().getName());
                        }
                ));
    }

    public Optional<JobHandler> getHandler(JobType type) {
        return Optional.ofNullable(handlerMap.get(type));
    }
}
