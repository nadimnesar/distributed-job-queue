package com.nadimnesar.jobqueue.common.dto;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;

public record ConsumedMessage(GetResponse getResponse,
                              Channel channel,
                              String jobId,
                              String queue,
                              String traceId) {
    public long getDeliveryTag() {
        return getResponse.getEnvelope().getDeliveryTag();
    }
}
