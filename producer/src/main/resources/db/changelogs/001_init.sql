CREATE TABLE job
(
    id                          UUID PRIMARY KEY,
    created_at                  TIMESTAMP   NOT NULL,
    updated_at                  TIMESTAMP   NOT NULL,
    priority                    VARCHAR(50) NOT NULL,
    status                      VARCHAR(50) NOT NULL,
    type                        VARCHAR(50),
    payload                     TEXT        NOT NULL,
    result                      TEXT,
    error_message               TEXT,
    current_progress            INT         NOT NULL,
    current_retry_attempt_count INT         NOT NULL,
    max_retry_attempt_count     INT         NOT NULL,
    started_at                  TIMESTAMP,
    completed_at                TIMESTAMP
);
