package com.nadimnesar.jobqueue.producer.service;

import com.nadimnesar.jobqueue.producer.dto.request.JobRequest;
import com.nadimnesar.jobqueue.producer.dto.response.JobResponse;
import com.nadimnesar.jobqueue.common.entity.JobEntity;

import java.util.List;

public interface JobService {
    JobEntity submitJob(JobRequest jobRequest);

    List<JobResponse> getAllJobs(int pageNumber, int pageSize);

    JobResponse getJobById(String jobId);
}
