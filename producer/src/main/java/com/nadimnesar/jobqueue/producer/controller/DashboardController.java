package com.nadimnesar.jobqueue.producer.controller;

import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import com.nadimnesar.jobqueue.producer.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/jobs/summary")
    public ResponseEntity<CommonResponse> getJobsSummary() {
        log.info("Received job summary request");
        var response = dashboardService.getJobsSummary();
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @GetMapping("/queue-metrics")
    public ResponseEntity<CommonResponse> getQueueMetrics() {
        log.info("Received queue metrics request");
        var response = dashboardService.getQueueMetrics();
        return ResponseEntity.status(response.getCode()).body(response);
    }
}
