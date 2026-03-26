CREATE DATABASE IF NOT EXISTS ai_hr_salary DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS salary_policy (
  id              VARCHAR(64) PRIMARY KEY,
  tenant_id        VARCHAR(64) NOT NULL,
  name            VARCHAR(128) NOT NULL,
  version         VARCHAR(32) NOT NULL,
  effective_from  DATE NOT NULL,
  effective_to    DATE NULL,
  status          VARCHAR(32) NOT NULL,
  policy_json     JSON NOT NULL,
  created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by      VARCHAR(64) NULL,
  UNIQUE KEY uk_tenant_name_version (tenant_id, name, version)
);

CREATE TABLE IF NOT EXISTS salary_calc_run (
  id              VARCHAR(64) PRIMARY KEY,
  tenant_id        VARCHAR(64) NOT NULL,
  pay_period      VARCHAR(16) NOT NULL,
  scope_type      VARCHAR(32) NOT NULL,
  scope_id        VARCHAR(64) NOT NULL,
  policy_id       VARCHAR(64) NOT NULL,
  calc_version    VARCHAR(32) NOT NULL,
  input_snapshot_id VARCHAR(64) NOT NULL,
  result_hash     VARCHAR(128) NOT NULL,
  status          VARCHAR(32) NOT NULL,
  created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by      VARCHAR(64) NULL,
  KEY idx_tenant_period (tenant_id, pay_period),
  KEY idx_scope (scope_type, scope_id)
);

CREATE TABLE IF NOT EXISTS salary_input_snapshot (
  id              VARCHAR(64) PRIMARY KEY,
  tenant_id        VARCHAR(64) NOT NULL,
  pay_period      VARCHAR(16) NOT NULL,
  snapshot_json   JSON NOT NULL,
  source_hash     VARCHAR(128) NOT NULL,
  created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by      VARCHAR(64) NULL,
  KEY idx_tenant_period (tenant_id, pay_period)
);

CREATE TABLE IF NOT EXISTS salary_result_line (
  id              VARCHAR(64) PRIMARY KEY,
  tenant_id        VARCHAR(64) NOT NULL,
  calc_run_id     VARCHAR(64) NOT NULL,
  employee_id     VARCHAR(64) NOT NULL,
  item_code       VARCHAR(64) NOT NULL,
  item_name       VARCHAR(128) NOT NULL,
  amount_cents    BIGINT NOT NULL,
  currency        VARCHAR(8) NOT NULL DEFAULT 'CNY',
  detail_json     JSON NULL,
  created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_calc_run (calc_run_id),
  KEY idx_emp (tenant_id, employee_id)
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

