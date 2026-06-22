package com.nadimnesar.jobqueue.common.dto;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import lombok.Builder;
import org.springframework.amqp.rabbit.connection.Connection;

@Builder
public record ConsumedMessage(GetResponse getResponse,
                              Channel channel,
                              Connection connection,
                              String jobId,
                              String queue,
                              String traceId) {
    public long getDeliveryTag() {
        return getResponse.getEnvelope().getDeliveryTag();
    }
}
