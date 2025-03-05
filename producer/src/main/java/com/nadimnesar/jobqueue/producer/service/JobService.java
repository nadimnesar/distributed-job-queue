package com.nadimnesar.jobqueue.producer.service;

import com.nadimnesar.jobqueue.common.dto.request.JobRequest;
import com.nadimnesar.jobqueue.common.dto.response.JobResponse;
import com.nadimnesar.jobqueue.producer.entity.JobEntity;

import java.util.List;

public interface JobService {
    JobEntity submitJob(JobRequest jobRequest);

    List<JobResponse> getAllJobs(int pageNumber, int pageSize);

    JobResponse getJobById(Long jobId);
}
