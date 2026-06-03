package com.nadimnesar.jobqueue.worker.service.impl;

import com.nadimnesar.jobqueue.worker.config.AppConfig.WorkerContext;
import com.nadimnesar.jobqueue.worker.service.HeartBeatService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HeartBeatServiceImpl implements HeartBeatService {
    private static final Logger logger = LoggerFactory.getLogger(HeartBeatServiceImpl.class);

    private final WorkerContext workerContext;

    @Override
    @Scheduled(cron = "${schedule.cron.heartbeat}")
    public void sendHeartbeat() {
        var workerHostname = workerContext.hostname();

        if (workerHostname == null) {
            logger.warn("Worker hostname is not initialized. Skipping heartbeat.");
            return;
        }

        logger.debug("Heartbeat sent for worker: {}, at: {}", workerHostname, LocalDateTime.now());
    }
}
