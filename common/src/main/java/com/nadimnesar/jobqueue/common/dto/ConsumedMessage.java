package com.nadimnesar.jobqueue.common.dto;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;

public record ConsumedMessage(GetResponse getResponse, Channel channel, String jobId, String queue) {
    public long getDeliveryTag() {
        return getResponse.getEnvelope().getDeliveryTag();
    }
}
