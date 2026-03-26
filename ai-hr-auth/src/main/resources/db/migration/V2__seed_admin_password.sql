-- Default demo login: tenantId=t-demo, username=admin, password=admin
UPDATE users SET password_bcrypt = '$2a$10$8K1p/a0dL1LXMIgoEDFrwOfMQ0cP/uRhl2L0U/jr5U4w5VvxY4rKq' WHERE id = 'u-admin';
