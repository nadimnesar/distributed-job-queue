package com.nadimnesar.jobqueue.worker.handler;

import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import com.nadimnesar.jobqueue.common.entity.JobEntity;

public interface JobHandler {
    void handle(JobEntity job) throws Exception;
    JobType getType();
}
