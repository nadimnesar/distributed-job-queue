package com.nadimnesar.jobqueue.producer.service;

import com.nadimnesar.jobqueue.producer.dto.CommonResponse;

public interface DashboardService {
    CommonResponse getJobsSummary();

    CommonResponse getQueueMetrics();
}
