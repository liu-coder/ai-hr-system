-- 演示账号：租户 t-demo，密码均为 admin（与 V2 中 admin 用户相同 bcrypt）
INSERT INTO users (id, tenant_id, username, password_bcrypt, display_name, enabled)
VALUES
  ('u-emp-002', 't-demo', 'emp002', '$2a$10$8K1p/a0dL1LXMIgoEDFrwOfMQ0cP/uRhl2L0U/jr5U4w5VvxY4rKq', '员工 emp-002', 1),
  ('u-emp-003', 't-demo', 'emp003', '$2a$10$8K1p/a0dL1LXMIgoEDFrwOfMQ0cP/uRhl2L0U/jr5U4w5VvxY4rKq', '员工 emp-003', 1),
  ('u-emp-004', 't-demo', 'emp004', '$2a$10$8K1p/a0dL1LXMIgoEDFrwOfMQ0cP/uRhl2L0U/jr5U4w5VvxY4rKq', '员工 emp-004', 1),
  ('u-emp-005', 't-demo', 'emp005', '$2a$10$8K1p/a0dL1LXMIgoEDFrwOfMQ0cP/uRhl2L0U/jr5U4w5VvxY4rKq', '员工 emp-005', 1);

INSERT INTO user_roles (user_id, role) VALUES
  ('u-emp-002', 'ROLE_USER'),
  ('u-emp-003', 'ROLE_USER'),
  ('u-emp-004', 'ROLE_USER'),
  ('u-emp-005', 'ROLE_USER');
