-- nadimnesar-001.1
CREATE TABLE job
(
    id                          UUID PRIMARY KEY,
    created_at                  TIMESTAMP   NOT NULL,
    updated_at                  TIMESTAMP   NOT NULL,
    priority                    VARCHAR(50) NOT NULL,
    status                      VARCHAR(50) NOT NULL,
    type                        VARCHAR(50) NOT NULL,
    payload                     TEXT        NOT NULL,
    result                      TEXT,
    error_message               TEXT,
    current_progress            INT         NOT NULL,
    current_retry_attempt_count INT         NOT NULL,
    max_retry_attempt_count     INT         NOT NULL,
    started_at                  TIMESTAMP,
    completed_at                TIMESTAMP
);

-- nadimnesar-001.2
CREATE TABLE job_dependency
(
    id            UUID PRIMARY KEY,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    job_id        UUID      NOT NULL,
    dependency_id UUID      NOT NULL,
    FOREIGN KEY (job_id) REFERENCES job (id),
    FOREIGN KEY (dependency_id) REFERENCES job (id),
    CONSTRAINT unique_job_dependency UNIQUE (job_id, dependency_id)
);

-- nadimnesar-001.3
ALTER TABLE job RENAME COLUMN current_retry_attempt_count TO current_attempt_count;
ALTER TABLE job RENAME COLUMN max_retry_attempt_count TO max_attempt_count;
