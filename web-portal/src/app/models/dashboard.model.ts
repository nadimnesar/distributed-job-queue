export interface JobsSummary {
  PENDING: number;
  PROCESSING: number;
  CANCELED: number;
  COMPLETED: number;
  FAILED: number;
  DEAD: number;
  TOTAL: number;
}

export interface QueueMetrics {
  queues: {
    HIGH_PRIORITY_QUEUE_LENGTH: number;
    MEDIUM_PRIORITY_QUEUE_LENGTH: number;
    LOW_PRIORITY_QUEUE_LENGTH: number;
    RETRY_QUEUE_LENGTH: number;
    DEAD_LETTER_QUEUE_LENGTH: number;
  };
}
