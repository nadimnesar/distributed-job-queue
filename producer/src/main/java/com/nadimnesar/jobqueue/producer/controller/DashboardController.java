package com.nadimnesar.jobqueue.producer.controller;

import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import com.nadimnesar.jobqueue.producer.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    @GetMapping("/jobs/summary")
    public ResponseEntity<CommonResponse> getJobsSummary() {
        logger.info("Received job summary request");
        try {
            var response = dashboardService.getJobsSummary();
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            logger.error("Error occurred while getting jobs summary: {}", e.getMessage());
            return ResponseEntity.ok().body(CommonResponse.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<CommonResponse> getQueueMetrics() {
        logger.info("Received queue metrics request");
        try {
            var response = dashboardService.getQueueMetrics();
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            logger.error("Error occurred while getting queue metrics: {}", e.getMessage());
            return ResponseEntity.ok().body(CommonResponse.badRequest(e.getMessage()));
        }
    }
}
