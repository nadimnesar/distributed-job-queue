package com.nadimnesar.jobqueue.common.constant;

public class DbConstant {
    private DbConstant() {
    }

    public static class BaseEntity {
        public static final String ID = "id";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
    }

    public static class Job {
        public static final String TABLE_NAME = "job";
        public static final String PRIORITY = "priority";
        public static final String STATUS = "status";
        public static final String TYPE = "type";
        public static final String PAYLOAD = "payload";
        public static final String RESULT = "result";
        public static final String ERROR_MESSAGE = "error_message";
        public static final String CURRENT_PROGRESS = "current_progress";
        public static final String CURRENT_RETRY_ATTEMPT_COUNT = "current_retry_attempt_count";
        public static final String MAX_RETRY_ATTEMPT_COUNT = "max_retry_attempt_count";
        public static final String STARTED_AT = "started_at";
        public static final String COMPLETED_AT = "completed_at";
    }

    public static class JobDependency {
        public static final String TABLE_NAME = "job_dependency";
        public static final String JOB_ID = "job_id";
        public static final String DEPENDENCY_ID = "dependency_id";
    }
}
