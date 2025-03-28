package com.nadimnesar.jobqueue.producer.controller;

import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import com.nadimnesar.jobqueue.producer.dto.request.JobRequest;
import com.nadimnesar.jobqueue.producer.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/job")
public class JobController {

    private static final Logger logger = LoggerFactory.getLogger(JobController.class);

    private final JobService jobService;

    @PostMapping("/create")
    public ResponseEntity<CommonResponse> submitJob(@RequestBody JobRequest jobRequest) {
        logger.info("JobController|Received submit job request: {}", jobRequest);

        try {
            var response = jobService.submitJob(jobRequest);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            logger.error("JobController|Error occurred while creating job: {}", e.getMessage());
            return ResponseEntity.ok().body(CommonResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<CommonResponse> cancelJob(@RequestParam String id) {
        logger.info("JobController|Received cancel job request with ID: {}", id);

        try {
            var response = jobService.cancelJob(id);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            logger.error("JobController|Error occurred while canceling job: {}", e.getMessage());
            return ResponseEntity.ok().body(CommonResponse.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<CommonResponse> getJobs(@RequestParam int pageNumber, @RequestParam int pageSize) {
        logger.info("JobController|Received get job request, pageNumber: {}, pageSize: {}", pageNumber, pageSize);

        try {
            var response = jobService.getAllJobs(pageNumber, pageSize);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            logger.error("JobController|Error occurred while getting jobs: {}", e.getMessage());
            return ResponseEntity.ok().body(CommonResponse.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/get")
    public ResponseEntity<CommonResponse> getJobs(@RequestParam String id) {
        logger.info("JobController|Received get job request, id: {}", id);

        try {
            var response = jobService.getJobById(id);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            logger.error("JobController|Error occurred while getting job: {}", e.getMessage());
            return ResponseEntity.ok().body(CommonResponse.badRequest(e.getMessage()));
        }
    }
}
