-- 考勤模块测试数据
USE ai_hr_attendance;

-- 插入考勤规则集
INSERT INTO attendance_rule_set (id, tenant_id, name, version, effective_from, effective_to, status, rule_json, created_at, created_by)
VALUES
('rule-set-001', 'tenant-001', '默认考勤规则', '1.0', '2024-01-01', NULL, 'ACTIVE', '{"rules": [{"name": "迟到规则", "type": "LATE_CHECKIN", "thresholdMinutes": 10}, {"name": "早退规则", "type": "EARLY_CHECKOUT", "thresholdMinutes": 10}, {"name": "旷工规则", "type": "ABSENT", "thresholdHours": 4}]}', CURRENT_TIMESTAMP, 'admin');

-- 插入打卡记录
INSERT INTO attendance_record (id, tenant_id, employee_id, work_date, check_in_at, check_out_at, source, raw_payload, created_at)
VALUES
('record-001', 'tenant-001', 'emp-001', '2024-03-25', '2024-03-25 09:00:00', '2024-03-25 18:00:00', 'APP', '{"deviceId": "device-001", "location": {"lat": 31.2304, "lng": 121.4737}}', CURRENT_TIMESTAMP),
('record-002', 'tenant-001', 'emp-001', '2024-03-26', '2024-03-26 09:30:00', '2024-03-26 17:30:00', 'APP', '{"deviceId": "device-001", "location": {"lat": 31.2304, "lng": 121.4737}}', CURRENT_TIMESTAMP),
('record-003', 'tenant-001', 'emp-002', '2024-03-25', '2024-03-25 08:50:00', '2024-03-25 18:10:00', 'APP', '{"deviceId": "device-002", "location": {"lat": 31.2304, "lng": 121.4737}}', CURRENT_TIMESTAMP);

-- 薪酬模块测试数据
USE ai_hr_salary;

-- 插入薪资政策
INSERT INTO salary_policy (id, tenant_id, name, version, effective_from, effective_to, status, policy_json, created_at, created_by)
VALUES
('policy-001', 'tenant-001', '默认薪资政策', '1.0', '2024-01-01', NULL, 'ACTIVE', '{"baseSalary": 10000, "allowances": [{"name": "交通补贴", "amount": 1000}, {"name": "餐补", "amount": 800}], "taxRate": 0.1}', CURRENT_TIMESTAMP, 'admin');

-- 插入薪资计算运行记录
INSERT INTO salary_input_snapshot (id, tenant_id, pay_period, snapshot_json, source_hash, created_at, created_by)
VALUES
('snapshot-001', 'tenant-001', '2024-03', '{"employees": [{"id": "emp-001", "baseSalary": 10000, "attendanceDays": 22, "overtimeHours": 10}, {"id": "emp-002", "baseSalary": 12000, "attendanceDays": 20, "overtimeHours": 5}]}', 'hash-001', CURRENT_TIMESTAMP, 'admin');

INSERT INTO salary_calc_run (id, tenant_id, pay_period, scope_type, scope_id, policy_id, calc_version, input_snapshot_id, result_hash, status, created_at, created_by)
VALUES
('calc-001', 'tenant-001', '2024-03', 'ALL', 'all', 'policy-001', '1.0', 'snapshot-001', 'result-hash-001', 'COMPLETED', CURRENT_TIMESTAMP, 'admin');

-- 插入薪资结果
INSERT INTO salary_result_line (id, tenant_id, calc_run_id, employee_id, item_code, item_name, amount_cents, currency, detail_json, created_at)
VALUES
('result-001', 'tenant-001', 'calc-001', 'emp-001', 'base_salary', '基本工资', 1000000, 'CNY', '{"rate": 1.0}', CURRENT_TIMESTAMP),
('result-002', 'tenant-001', 'calc-001', 'emp-001', 'traffic_allowance', '交通补贴', 100000, 'CNY', NULL, CURRENT_TIMESTAMP),
('result-003', 'tenant-001', 'calc-001', 'emp-001', 'meal_allowance', '餐补', 80000, 'CNY', NULL, CURRENT_TIMESTAMP),
('result-004', 'tenant-001', 'calc-001', 'emp-001', 'overtime_pay', '加班费', 50000, 'CNY', '{"hours": 10, "rate": 50}', CURRENT_TIMESTAMP),
('result-005', 'tenant-001', 'calc-001', 'emp-001', 'tax', '个人所得税', 123000, 'CNY', '{"rate": 0.1}', CURRENT_TIMESTAMP),
('result-006', 'tenant-001', 'calc-001', 'emp-002', 'base_salary', '基本工资', 1200000, 'CNY', '{"rate": 1.0}', CURRENT_TIMESTAMP),
('result-007', 'tenant-001', 'calc-001', 'emp-002', 'traffic_allowance', '交通补贴', 100000, 'CNY', NULL, CURRENT_TIMESTAMP),
('result-008', 'tenant-001', 'calc-001', 'emp-002', 'meal_allowance', '餐补', 80000, 'CNY', NULL, CURRENT_TIMESTAMP),
('result-009', 'tenant-001', 'calc-001', 'emp-002', 'overtime_pay', '加班费', 25000, 'CNY', '{"hours": 5, "rate": 50}', CURRENT_TIMESTAMP),
('result-010', 'tenant-001', 'calc-001', 'emp-002', 'tax', '个人所得税', 140500, 'CNY', '{"rate": 0.1}', CURRENT_TIMESTAMP);
