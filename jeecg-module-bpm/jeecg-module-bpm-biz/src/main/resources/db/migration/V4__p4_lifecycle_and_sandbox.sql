-- P4: category index + sandbox run table
-- Note: bpm_process_definition.category column already exists from P1
-- Just add the index and sandbox table

CREATE INDEX idx_def_category_state ON bpm_process_definition (category, state);

CREATE TABLE IF NOT EXISTS bpm_sandbox_run (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    def_id_draft VARCHAR(64) NOT NULL,
    runner_id    BIGINT NOT NULL,
    result       VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
    log          MEDIUMTEXT NULL,
    start_time   DATETIME NOT NULL,
    end_time     DATETIME NULL,
    INDEX idx_sandbox_def (def_id_draft),
    INDEX idx_sandbox_runner (runner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
