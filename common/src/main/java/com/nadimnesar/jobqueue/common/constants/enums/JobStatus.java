package com.nadimnesar.jobqueue.common.constants.enums;

/**
 * Represents the lifecycle status of a job within the distributed job queue system.
 * <p>
 * A job transitions through these states as it is created, picked up by a worker,
 * processed, and ultimately reaches a terminal state. The typical flow is:
 * {@link #PENDING} &rarr; {@link #PROCESSING} &rarr; {@link #COMPLETED} (success)
 * or {@link #FAILED} (recoverable error) or {@link #CANCELED} (invalid payload).
 * If retries are exhausted, a {@link #FAILED} job moves to {@link #DEAD}.
 *
 * @author nadimnesar
 */
public enum JobStatus {
    PENDING,
    PROCESSING,
    CANCELED,
    COMPLETED,
    FAILED,
    DEAD
}
