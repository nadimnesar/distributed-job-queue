package com.nadimnesar.jobqueue.worker.handler.impl;

import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.worker.handler.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailJobHandler implements JobHandler {

    private static final Logger logger = LoggerFactory.getLogger(EmailJobHandler.class);

    @Override
    public void handle(JobEntity job) {
        logger.info("Processing email job with id={}, payload={}", job.getId(), job.getPayload());
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            // Restore the interrupt status so callers can detect it
            Thread.currentThread().interrupt();
            logger.warn("Email job with id={} was interrupted, stopping gracefully", job.getId());
            return;
        }
        logger.info("Completed email sending job with id={}", job.getId());
    }

    @Override
    public JobType getType() {
        return JobType.EMAIL_SENDING;
    }
}
