package com.nadimnesar.jobqueue.worker.service;

public interface JobProcessorService {
    void processJob(String jobId);
}
