-- nadimnesar-002.1
ALTER TABLE job DROP COLUMN error_message;

-- nadimnesar-002.2
ALTER TABLE job DROP COLUMN current_progress;

-- nadimnesar-002.3
ALTER TABLE job RENAME COLUMN current_attempt_count TO attempt_count;
