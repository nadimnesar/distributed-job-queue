package com.nadimnesar.jobqueue.producer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@Builder
public class CommonResponse {

    @Builder.Default
    private String message = "Operation Successful.";

    @Builder.Default
    private int code = 200;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object data;

    public static CommonResponse badRequest(String message) {
        return CommonResponse.builder()
                .message(message)
                .code(HttpStatus.BAD_REQUEST.value())
                .build();
    }
}
