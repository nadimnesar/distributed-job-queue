package com.nadimnesar.jobqueue.producer.controller;

import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import com.nadimnesar.jobqueue.producer.dto.request.JobRequest;
import com.nadimnesar.jobqueue.producer.service.JobService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class JobController {

    private final JobService jobService;

    @PostMapping("/job/create")
    public ResponseEntity<CommonResponse> submitJob(@Valid @RequestBody JobRequest jobRequest) {
        log.info("Received submit job request: {}", jobRequest);
        var response = jobService.submitJob(jobRequest);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @PostMapping("/jobs/{id}/cancel")
    public ResponseEntity<CommonResponse> cancelJob(@PathVariable UUID id) {
        log.info("Received cancel job request with ID: {}", id);
        var response = jobService.cancelJob(id.toString());
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @GetMapping("/jobs")
    public ResponseEntity<CommonResponse> getJobs(
            @RequestParam @Min(0) int page,
            @RequestParam @Min(1) @Max(200) int size) {
        log.info("Received get job request, pageNumber: {}, pageSize: {}", page, size);
        var response = jobService.getAllJobs(page, size);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<CommonResponse> getJob(@PathVariable UUID id) {
        log.info("Received get job request, id: {}", id);
        var response = jobService.getJobById(id.toString());
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @GetMapping("/jobs/filter")
    public ResponseEntity<CommonResponse> getJobsByStatusAndType(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        log.info("Received get jobs request with status: {}, type: {}, page: {}, size: {}", status, type, page, size);

        boolean hasStatus = status != null && !status.isBlank();
        boolean hasType = type != null && !type.isBlank();

        if (!hasStatus && !hasType) {
            CommonResponse response = CommonResponse.badRequest("At least one of 'status' or 'type' must be provided");
            return ResponseEntity.status(response.getCode()).body(response);
        }

        var pageable = PageRequest.of(page, size);
        CommonResponse response;
        if (hasStatus && hasType) {
            response = jobService.getJobsByStatusAndType(status, type, pageable);
        } else if (hasStatus) {
            response = jobService.getJobByStatus(status, pageable);
        } else {
            response = jobService.getJobsByType(type, pageable);
        }

        return ResponseEntity.status(response.getCode()).body(response);
    }

    @PostMapping("/jobs/{id}/revive")
    public ResponseEntity<CommonResponse> reviveDeadJobById(@PathVariable UUID id) {
        log.info("Received revive dead job request with ID: {}", id);
        var response = jobService.reviveDeadJobById(id.toString());
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @PostMapping("/jobs/revive")
    public ResponseEntity<CommonResponse> reviveDeadJobs() {
        log.info("Received revive dead jobs request");
        var response = jobService.reviveAllDeadJobs();
        return ResponseEntity.status(response.getCode()).body(response);
    }
}
