-- AI chat demo seed data
-- Tenant: t-demo
-- Employees: E1001, E1002, E1003

USE ai_hr_attendance;

INSERT INTO attendance_rule_set (id, tenant_id, name, version, effective_from, effective_to, status, rule_json, created_by)
VALUES
  ('ars-demo-v1', 't-demo', '标准考勤规则', 'v1', '2026-01-01', NULL, 'ACTIVE',
   JSON_OBJECT('lateAfterMinutes', 10, 'earlyLeaveBeforeMinutes', 10, 'requiredWorkHours', 8),
   'seed-script')
ON DUPLICATE KEY UPDATE status = VALUES(status), rule_json = VALUES(rule_json);

INSERT INTO attendance_record
  (id, tenant_id, employee_id, work_date, check_in_at, check_out_at, source, raw_payload,
   check_in_location, check_out_location, check_in_location_valid, check_out_location_valid,
   check_in_device_type, check_out_device_type)
VALUES
  ('ar-e1001-20260301', 't-demo', 'E1001', '2026-03-01', '2026-03-01 09:03:00', '2026-03-01 18:08:00', 'APP',
   JSON_OBJECT('shift', 'A'), '上海总部', '上海总部', b'1', b'1', 'MOBILE', 'MOBILE'),
  ('ar-e1001-20260302', 't-demo', 'E1001', '2026-03-02', '2026-03-02 09:31:00', '2026-03-02 18:02:00', 'APP',
   JSON_OBJECT('shift', 'A'), '上海总部', '上海总部', b'1', b'1', 'MOBILE', 'MOBILE'),
  ('ar-e1001-20260303', 't-demo', 'E1001', '2026-03-03', '2026-03-03 08:58:00', '2026-03-03 16:42:00', 'APP',
   JSON_OBJECT('shift', 'A'), '上海总部', '上海总部', b'1', b'1', 'MOBILE', 'MOBILE'),
  ('ar-e1002-20260301', 't-demo', 'E1002', '2026-03-01', '2026-03-01 09:02:00', '2026-03-01 19:10:00', 'APP',
   JSON_OBJECT('shift', 'A'), '杭州分部', '杭州分部', b'1', b'1', 'WEB', 'WEB'),
  ('ar-e1003-20260301', 't-demo', 'E1003', '2026-03-01', '2026-03-01 10:12:00', '2026-03-01 18:01:00', 'APP',
   JSON_OBJECT('shift', 'A'), '苏州分部', '苏州分部', b'1', b'1', 'MOBILE', 'WEB')
ON DUPLICATE KEY UPDATE
  check_in_at = VALUES(check_in_at),
  check_out_at = VALUES(check_out_at),
  raw_payload = VALUES(raw_payload);

INSERT INTO attendance_anomaly
  (id, tenant_id, employee_id, work_date, type, severity, rule_hit, evidence_json, snapshot_id)
VALUES
  ('aa-e1001-20260302-late', 't-demo', 'E1001', '2026-03-02', 'LATE', 'MEDIUM',
   'lateAfterMinutes>10', JSON_OBJECT('lateMinutes', 21, 'checkIn', '09:31:00'), 'snap-e1001-202603'),
  ('aa-e1001-20260303-early', 't-demo', 'E1001', '2026-03-03', 'EARLY_LEAVE', 'LOW',
   'earlyLeaveBeforeMinutes>10', JSON_OBJECT('earlyLeaveMinutes', 78, 'checkOut', '16:42:00'), 'snap-e1001-202603'),
  ('aa-e1003-20260301-late', 't-demo', 'E1003', '2026-03-01', 'LATE', 'HIGH',
   'lateAfterMinutes>10', JSON_OBJECT('lateMinutes', 72, 'checkIn', '10:12:00'), 'snap-e1003-202603')
ON DUPLICATE KEY UPDATE severity = VALUES(severity), evidence_json = VALUES(evidence_json);


USE ai_hr_salary;

INSERT INTO salary_policy (id, tenant_id, name, version, effective_from, effective_to, status, policy_json, created_by)
VALUES
  ('sp-demo-v1', 't-demo', '标准薪酬策略', 'v1', '2026-01-01', NULL, 'ACTIVE',
   JSON_OBJECT('base', 1200000, 'attendancePenaltyPerDay', 8000, 'taxRate', 0.1), 'seed-script')
ON DUPLICATE KEY UPDATE status = VALUES(status), policy_json = VALUES(policy_json);

INSERT INTO salary_input_snapshot (id, tenant_id, pay_period, snapshot_json, source_hash, created_by)
VALUES
  ('sis-2026-03', 't-demo', '2026-03',
   JSON_OBJECT(
     'employees',
     JSON_ARRAY(
       JSON_OBJECT('employeeId', 'E1001', 'lateDays', 1, 'earlyLeaveDays', 1),
       JSON_OBJECT('employeeId', 'E1002', 'lateDays', 0, 'earlyLeaveDays', 0),
       JSON_OBJECT('employeeId', 'E1003', 'lateDays', 1, 'earlyLeaveDays', 0)
     )
   ),
   'seed-hash-2026-03',
   'seed-script')
ON DUPLICATE KEY UPDATE snapshot_json = VALUES(snapshot_json);

INSERT INTO salary_calc_run
  (id, tenant_id, pay_period, scope_type, scope_id, policy_id, calc_version, input_snapshot_id, result_hash, status, created_by)
VALUES
  ('scr-2026-03-e1001', 't-demo', '2026-03', 'EMPLOYEE', 'E1001', 'sp-demo-v1', 'v1', 'sis-2026-03', 'hash-e1001', 'SUCCESS', 'seed-script'),
  ('scr-2026-03-e1002', 't-demo', '2026-03', 'EMPLOYEE', 'E1002', 'sp-demo-v1', 'v1', 'sis-2026-03', 'hash-e1002', 'SUCCESS', 'seed-script'),
  ('scr-2026-03-e1003', 't-demo', '2026-03', 'EMPLOYEE', 'E1003', 'sp-demo-v1', 'v1', 'sis-2026-03', 'hash-e1003', 'SUCCESS', 'seed-script')
ON DUPLICATE KEY UPDATE status = VALUES(status), result_hash = VALUES(result_hash);

INSERT INTO salary_result_line
  (id, tenant_id, calc_run_id, employee_id, item_code, item_name, amount_cents, currency, detail_json)
VALUES
  ('srl-e1001-base', 't-demo', 'scr-2026-03-e1001', 'E1001', 'BASE', '基础工资', 1200000, 'CNY', JSON_OBJECT('remark', '标准月薪')),
  ('srl-e1001-penalty', 't-demo', 'scr-2026-03-e1001', 'E1001', 'ATT_PENALTY', '考勤扣款', -16000, 'CNY', JSON_OBJECT('lateDays', 1, 'earlyLeaveDays', 1)),
  ('srl-e1001-net', 't-demo', 'scr-2026-03-e1001', 'E1001', 'NET', '实发工资', 1065600, 'CNY', JSON_OBJECT('tax', 118400)),
  ('srl-e1002-base', 't-demo', 'scr-2026-03-e1002', 'E1002', 'BASE', '基础工资', 1300000, 'CNY', JSON_OBJECT('remark', '绩效加成后')),
  ('srl-e1002-net', 't-demo', 'scr-2026-03-e1002', 'E1002', 'NET', '实发工资', 1170000, 'CNY', JSON_OBJECT('tax', 130000)),
  ('srl-e1003-base', 't-demo', 'scr-2026-03-e1003', 'E1003', 'BASE', '基础工资', 1100000, 'CNY', JSON_OBJECT('remark', '标准月薪')),
  ('srl-e1003-penalty', 't-demo', 'scr-2026-03-e1003', 'E1003', 'ATT_PENALTY', '考勤扣款', -8000, 'CNY', JSON_OBJECT('lateDays', 1)),
  ('srl-e1003-net', 't-demo', 'scr-2026-03-e1003', 'E1003', 'NET', '实发工资', 982800, 'CNY', JSON_OBJECT('tax', 109200))
ON DUPLICATE KEY UPDATE amount_cents = VALUES(amount_cents), detail_json = VALUES(detail_json);


USE ai_hr_aicore;

INSERT INTO policy_document
  (id, tenant_id, doc_type, title, version, security_level, effective_from, effective_to, source_uri, created_by)
VALUES
  ('pd-att-001', 't-demo', 'ATTENDANCE', '考勤与异常处理规范', 'v1', 'NORMAL', '2026-01-01', NULL, 'seed://attendance-policy', 'seed-script'),
  ('pd-sal-001', 't-demo', 'SALARY', '薪酬核算与扣款规则', 'v1', 'NORMAL', '2026-01-01', NULL, 'seed://salary-policy', 'seed-script')
ON DUPLICATE KEY UPDATE title = VALUES(title), version = VALUES(version);

INSERT INTO policy_chunk
  (id, tenant_id, document_id, chunk_index, content, metadata_json, embedding)
VALUES
  ('pc-att-001-1', 't-demo', 'pd-att-001', 1,
   '迟到超过10分钟记为异常，早退超过10分钟记为异常；当月累计异常达到3次需发起主管复核。',
   JSON_OBJECT('tags', JSON_ARRAY('attendance', 'late', 'early_leave')), NULL),
  ('pc-att-001-2', 't-demo', 'pd-att-001', 2,
   '考勤异常会影响当月绩效与薪酬扣款，单次异常默认扣款80元。',
   JSON_OBJECT('tags', JSON_ARRAY('attendance', 'salary_link')), NULL),
  ('pc-sal-001-1', 't-demo', 'pd-sal-001', 1,
   '薪酬计算以基础工资为核心，考勤异常按规则扣减，税率按10%模拟。',
   JSON_OBJECT('tags', JSON_ARRAY('salary', 'calc', 'tax')), NULL)
ON DUPLICATE KEY UPDATE content = VALUES(content), metadata_json = VALUES(metadata_json);

INSERT INTO chat_session (id, tenant_id, user_id, title)
VALUES
  ('sess-demo-001', 't-demo', 'u-admin', '3月考勤与薪酬分析'),
  ('sess-demo-002', 't-demo', 'u-admin', '异常复核建议')
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO chat_message (id, tenant_id, session_id, role, content, trace_id)
VALUES
  ('msg-demo-001', 't-demo', 'sess-demo-001', 'USER', '请分析E1001在2026-03的考勤异常', 'seed-trace-001'),
  ('msg-demo-002', 't-demo', 'sess-demo-001', 'ASSISTANT', 'E1001存在1次迟到和1次早退，建议复核。', 'seed-trace-001')
ON DUPLICATE KEY UPDATE content = VALUES(content);

INSERT INTO tool_call_log
  (id, tenant_id, session_id, tool_name, request_json, response_json, status, trace_id)
VALUES
  ('tcl-demo-001', 't-demo', 'sess-demo-001', 'attendance.queryAnomalies',
   JSON_OBJECT('employeeId', 'E1001', 'start', '2026-03-01', 'end', '2026-03-31'),
   JSON_OBJECT('count', 2, 'types', JSON_ARRAY('LATE', 'EARLY_LEAVE')),
   'SUCCESS', 'seed-trace-001')
ON DUPLICATE KEY UPDATE response_json = VALUES(response_json), status = VALUES(status);
