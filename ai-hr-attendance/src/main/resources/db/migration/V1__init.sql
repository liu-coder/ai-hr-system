CREATE DATABASE IF NOT EXISTS ai_hr_attendance DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS attendance_rule_set (
  id              VARCHAR(64) PRIMARY KEY,
  tenant_id        VARCHAR(64) NOT NULL,
  name            VARCHAR(128) NOT NULL,
  version         VARCHAR(32) NOT NULL,
  effective_from  DATE NOT NULL,
  effective_to    DATE NULL,
  status          VARCHAR(32) NOT NULL,
  rule_json       JSON NOT NULL,
  created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by      VARCHAR(64) NULL,
  UNIQUE KEY uk_tenant_name_version (tenant_id, name, version)
);

CREATE TABLE IF NOT EXISTS attendance_record (
  id            VARCHAR(64) PRIMARY KEY,
  tenant_id      VARCHAR(64) NOT NULL,
  employee_id   VARCHAR(64) NOT NULL,
  work_date     DATE NOT NULL,
  check_in_at   DATETIME(6) NULL,
  check_out_at  DATETIME(6) NULL,
  source        VARCHAR(32) NOT NULL,
  raw_payload   JSON NULL,
  created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_tenant_emp_date (tenant_id, employee_id, work_date)
);

CREATE TABLE IF NOT EXISTS attendance_snapshot (
  id              VARCHAR(64) PRIMARY KEY,
  tenant_id        VARCHAR(64) NOT NULL,
  employee_id     VARCHAR(64) NOT NULL,
  period_start    DATE NOT NULL,
  period_end      DATE NOT NULL,
  rule_set_id     VARCHAR(64) NOT NULL,
  source_hash     VARCHAR(128) NOT NULL,
  snapshot_json   JSON NOT NULL,
  created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by      VARCHAR(64) NULL,
  KEY idx_tenant_emp_period (tenant_id, employee_id, period_start, period_end)
);

CREATE TABLE IF NOT EXISTS attendance_anomaly (
  id            VARCHAR(64) PRIMARY KEY,
  tenant_id      VARCHAR(64) NOT NULL,
  employee_id   VARCHAR(64) NOT NULL,
  work_date     DATE NOT NULL,
  type          VARCHAR(64) NOT NULL,
  severity      VARCHAR(32) NOT NULL,
  rule_hit      VARCHAR(128) NOT NULL,
  evidence_json JSON NOT NULL,
  snapshot_id   VARCHAR(64) NOT NULL,
  created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_tenant_emp_date (tenant_id, employee_id, work_date)
);

CREATE TABLE IF NOT EXISTS audit_event (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id      VARCHAR(64) NOT NULL,
  actor_user_id VARCHAR(64) NULL,
  actor_type    VARCHAR(32) NOT NULL,
  action        VARCHAR(128) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id   VARCHAR(128) NULL,
  trace_id      VARCHAR(128) NULL,
  occurred_at   DATETIME(6) NOT NULL,
  attributes    JSON NULL,
  KEY idx_tenant_time (tenant_id, occurred_at),
  KEY idx_trace (trace_id)
);

