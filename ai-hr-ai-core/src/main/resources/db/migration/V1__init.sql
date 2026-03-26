CREATE DATABASE IF NOT EXISTS ai_hr_aicore DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS policy_document (
  id            VARCHAR(64) PRIMARY KEY,
  tenant_id      VARCHAR(64) NOT NULL,
  doc_type      VARCHAR(64) NOT NULL,
  title         VARCHAR(256) NOT NULL,
  version       VARCHAR(32) NOT NULL,
  security_level VARCHAR(32) NOT NULL,
  effective_from DATE NOT NULL,
  effective_to   DATE NULL,
  source_uri    VARCHAR(512) NULL,
  created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by    VARCHAR(64) NULL,
  UNIQUE KEY uk_tenant_type_version (tenant_id, doc_type, version)
);

CREATE TABLE IF NOT EXISTS policy_chunk (
  id            VARCHAR(64) PRIMARY KEY,
  tenant_id      VARCHAR(64) NOT NULL,
  document_id   VARCHAR(64) NOT NULL,
  chunk_index   INT NOT NULL,
  content       TEXT NOT NULL,
  metadata_json JSON NOT NULL,
  embedding     JSON NULL,
  created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_doc (document_id)
);

CREATE TABLE IF NOT EXISTS chat_session (
  id            VARCHAR(64) PRIMARY KEY,
  tenant_id      VARCHAR(64) NOT NULL,
  user_id       VARCHAR(64) NOT NULL,
  title         VARCHAR(256) NULL,
  created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  KEY idx_user (tenant_id, user_id)
);

CREATE TABLE IF NOT EXISTS chat_message (
  id            VARCHAR(64) PRIMARY KEY,
  tenant_id      VARCHAR(64) NOT NULL,
  session_id    VARCHAR(64) NOT NULL,
  role          VARCHAR(16) NOT NULL,
  content       MEDIUMTEXT NOT NULL,
  trace_id      VARCHAR(128) NULL,
  created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_session_time (session_id, created_at)
);

CREATE TABLE IF NOT EXISTS tool_call_log (
  id            VARCHAR(64) PRIMARY KEY,
  tenant_id      VARCHAR(64) NOT NULL,
  session_id    VARCHAR(64) NOT NULL,
  tool_name     VARCHAR(128) NOT NULL,
  request_json  JSON NOT NULL,
  response_json JSON NULL,
  status        VARCHAR(32) NOT NULL,
  trace_id      VARCHAR(128) NULL,
  created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_session (session_id),
  KEY idx_trace (trace_id)
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

