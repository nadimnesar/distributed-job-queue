package com.nadimnesar.jobqueue.producer.controller;

import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import com.nadimnesar.jobqueue.producer.dto.request.JobRequest;
import com.nadimnesar.jobqueue.producer.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

            return ResponseEntity.ok().body(CommonResponse.builder()
                    .message("Job is created successfully.")
                    .data(response)
                    .code(HttpStatus.CREATED.value())
                    .build());
        } catch (Exception e) {
            logger.error("JobController|Error occurred while creating job: {}", e.getMessage());
            return ResponseEntity.ok().body(CommonResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<CommonResponse> cancelJob(@RequestParam String id) {
        logger.info("JobController|Received cancel job request with ID: {}", id);
        return ResponseEntity.ok().body(jobService.cancelJob(id));
    }

    @GetMapping("/get-all")
    public ResponseEntity<CommonResponse> getJobs(@RequestParam int pageNumber, @RequestParam int pageSize) {
        logger.info("JobController|Received get job request, pageNumber: {}, pageSize: {}", pageNumber, pageSize);

        var response = jobService.getAllJobs(pageNumber, pageSize);

        if (response.isEmpty()) {
            return ResponseEntity.ok().body(CommonResponse.notFound("No jobs found."));
        } else {
            return ResponseEntity.ok().body(CommonResponse.builder()
                    .data(response)
                    .build());
        }
    }

    @GetMapping("/get")
    public ResponseEntity<CommonResponse> getJobs(@RequestParam String id) {
        logger.info("JobController|Received get job request, id: {}", id);

        var response = jobService.getJobById(id);

        try {
            if (response == null) {
                return ResponseEntity.ok().body(CommonResponse.notFound("No job found with given id."));
            } else {
                return ResponseEntity.ok().body(CommonResponse.builder()
                        .data(response)
                        .build());
            }
        } catch (Exception e) {
            logger.error("JobController|Error occurred while getting job: {}", e.getMessage());
            return ResponseEntity.ok().body(CommonResponse.badRequest(e.getMessage()));
        }
    }
}
