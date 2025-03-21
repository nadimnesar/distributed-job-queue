package com.nadimnesar.jobqueue.producer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommonResponse {

    @Builder.Default
    private String message = "Operation Successful.";

    @Builder.Default
    private int code = 200;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object data;
}
