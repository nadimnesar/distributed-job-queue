package com.nadimnesar.jobqueue.producer.service;

import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import com.nadimnesar.jobqueue.producer.dto.request.JobRequest;

public interface JobService {
    CommonResponse submitJob(JobRequest jobRequest);

    CommonResponse getAllJobs(int pageNumber, int pageSize);

    CommonResponse getJobById(String jobId);

    CommonResponse cancelJob(String jobId);

    CommonResponse retryDeadJobs();
}
