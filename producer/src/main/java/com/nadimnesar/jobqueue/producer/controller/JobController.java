package com.nadimnesar.jobqueue.producer.controller;

import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import com.nadimnesar.jobqueue.producer.dto.request.JobRequest;
import com.nadimnesar.jobqueue.producer.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class JobController {

    private static final Logger logger = LoggerFactory.getLogger(JobController.class);

    private final JobService jobService;

    @PostMapping("/job/create")
    public ResponseEntity<CommonResponse> submitJob(@Valid @RequestBody JobRequest jobRequest) {
        logger.info("Received submit job request: {}", jobRequest);

        try {
            var response = jobService.submitJob(jobRequest);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            logger.error("Error occurred while creating job: {}", e.getMessage());
            return ResponseEntity.ok().body(CommonResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/job/cancel")
    public ResponseEntity<CommonResponse> cancelJob(@RequestParam String id) {
        logger.info("Received cancel job request with ID: {}", id);

        try {
            var response = jobService.cancelJob(id);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            logger.error("Error occurred while canceling job: {}", e.getMessage());
            return ResponseEntity.ok().body(CommonResponse.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/jobs")
    public ResponseEntity<CommonResponse> getJobs(@RequestParam int page, @RequestParam int size) {
        logger.info("Received get job request, pageNumber: {}, pageSize: {}", page, size);

        try {
            var response = jobService.getAllJobs(page, size);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            logger.error("Error occurred while getting jobs: {}", e.getMessage());
            return ResponseEntity.ok().body(CommonResponse.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/job")
    public ResponseEntity<CommonResponse> getJob(@RequestParam String id) {
        logger.info("Received get job request, id: {}", id);

        try {
            var response = jobService.getJobById(id);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            logger.error("Error occurred while getting job: {}", e.getMessage());
            return ResponseEntity.ok().body(CommonResponse.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/jobs/filter")
    public ResponseEntity<CommonResponse> getJobsByStatusAndType(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        logger.info("Received get jobs request with status: {}, type: {}", status, type);

        try {
            boolean hasStatus = status != null && !status.isBlank();
            boolean hasType = type != null && !type.isBlank();

            CommonResponse response;
            if (hasStatus && hasType) {
                response = jobService.getJobsByStatusAndType(status, type);
            } else if (hasStatus) {
                response = jobService.getJobByStatus(status);
            } else if (hasType) {
                response = jobService.getJobsByType(type);
            } else {
                return ResponseEntity.ok().body(CommonResponse.badRequest("At least one of 'status' or 'type' must be provided"));
            }

            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            logger.error("Error occurred while filtering jobs: {}", e.getMessage());
            return ResponseEntity.ok().body(CommonResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/job/revive")
    public ResponseEntity<CommonResponse> reviveDeadJobById(@RequestParam String id) {
        logger.info("Received revive dead job request with ID: {}", id);

        try {
            var response = jobService.reviveDeadJobById(id);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            logger.error("Error occurred while reviving dead job: {}", e.getMessage());
            return ResponseEntity.ok().body(CommonResponse.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/jobs/revive")
    public ResponseEntity<CommonResponse> reviveDeadJobs() {
        logger.info("Received revive dead jobs request");

        try {
            var response = jobService.reviveAllDeadJobs();
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            logger.error("Error occurred while reviving dead jobs: {}", e.getMessage());
            return ResponseEntity.ok().body(CommonResponse.badRequest(e.getMessage()));
        }
    }
}
