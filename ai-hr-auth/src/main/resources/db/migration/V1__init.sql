CREATE DATABASE IF NOT EXISTS ai_hr_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS users (
  id           VARCHAR(64) PRIMARY KEY,
  tenant_id    VARCHAR(64) NOT NULL,
  username     VARCHAR(128) NOT NULL,
  password_bcrypt VARCHAR(255) NOT NULL,
  display_name VARCHAR(128) NULL,
  enabled      TINYINT NOT NULL DEFAULT 1,
  created_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_tenant_username (tenant_id, username)
);

CREATE TABLE IF NOT EXISTS user_roles (
  user_id VARCHAR(64) NOT NULL,
  role    VARCHAR(64) NOT NULL,
  PRIMARY KEY (user_id, role)
);

INSERT INTO users (id, tenant_id, username, password_bcrypt, display_name, enabled)
VALUES ('u-admin', 't-demo', 'admin', '$2a$10$h2UQqWQb8G7m4tqEwzjJ4u4eLZx8yC4JtXcL9pWfE9L8o6Hj8h4cK', 'Admin', 1)
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO user_roles (user_id, role)
VALUES ('u-admin', 'ROLE_ADMIN')
ON DUPLICATE KEY UPDATE role=role;

