export type JobPriority = 'HIGH' | 'MEDIUM' | 'LOW';
export type JobType = 'EMAIL_SENDING' | 'PAYMENT_SENDING';
export type JobStatus = 'PENDING' | 'PROCESSING' | 'CANCELED' | 'COMPLETED' | 'FAILED' | 'DEAD';

export interface JobRequest {
  priority: JobPriority;
  type: JobType;
  payload: string;
  dependencies?: string[];
  maxAttemptCount?: number;
}

export interface Job {
  id: string;
  priority: JobPriority;
  type: JobType;
  status: JobStatus;
  dependents: string[];
  dependencies: string[];
  result?: string;
  attemptCount?: number;
  maxAttemptCount?: number;
  startedAt?: string;
  completedAt?: string;
}

export interface CommonResponse<T> {
  message: string;
  code: number;
  data: T;
  traceId?: string;
  spanId?: string;
}

export const JOB_STATUSES: JobStatus[] = ['PENDING', 'PROCESSING', 'CANCELED', 'COMPLETED', 'FAILED', 'DEAD'];
export const JOB_PRIORITIES: JobPriority[] = ['HIGH', 'MEDIUM', 'LOW'];
export const JOB_TYPES: JobType[] = ['EMAIL_SENDING', 'PAYMENT_SENDING'];
