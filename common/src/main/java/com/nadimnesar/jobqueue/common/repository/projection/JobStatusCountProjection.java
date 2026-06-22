package com.nadimnesar.jobqueue.common.repository.projection;

import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;

public interface JobStatusCountProjection {
    JobStatus getStatus();
    Long getCount();
}
