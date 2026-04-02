-- 批量演示：t-demo 下 emp-002～emp-005 打卡记录 + 快照 + 异常（2026-03）
INSERT INTO attendance_snapshot (
  id, tenant_id, employee_id, period_start, period_end, rule_set_id, source_hash, snapshot_json, created_at, created_by
) VALUES
  ('as-demo-emp002-202603', 't-demo', 'emp-002', '2026-03-01', '2026-03-31', 'ars-demo-tdemo-v1', 'batch-hash-002', '{}', CURRENT_TIMESTAMP(6), 'seed'),
  ('as-demo-emp003-202603', 't-demo', 'emp-003', '2026-03-01', '2026-03-31', 'ars-demo-tdemo-v1', 'batch-hash-003', '{}', CURRENT_TIMESTAMP(6), 'seed'),
  ('as-demo-emp004-202603', 't-demo', 'emp-004', '2026-03-01', '2026-03-31', 'ars-demo-tdemo-v1', 'batch-hash-004', '{}', CURRENT_TIMESTAMP(6), 'seed'),
  ('as-demo-emp005-202603', 't-demo', 'emp-005', '2026-03-01', '2026-03-31', 'ars-demo-tdemo-v1', 'batch-hash-005', '{}', CURRENT_TIMESTAMP(6), 'seed');

INSERT INTO attendance_record (
  id, tenant_id, employee_id, work_date, check_in_at, check_out_at, source, raw_payload, created_at
) VALUES
  ('ar-batch-002a', 't-demo', 'emp-002', '2026-03-06', '2026-03-06 09:00:00.000000', '2026-03-06 18:10:00.000000', 'MANUAL', NULL, CURRENT_TIMESTAMP(6)),
  ('ar-batch-002b', 't-demo', 'emp-002', '2026-03-07', '2026-03-07 09:25:00.000000', '2026-03-07 18:00:00.000000', 'MANUAL', NULL, CURRENT_TIMESTAMP(6)),
  ('ar-batch-003a', 't-demo', 'emp-003', '2026-03-08', '2026-03-08 08:55:00.000000', NULL, 'MANUAL', NULL, CURRENT_TIMESTAMP(6)),
  ('ar-batch-003b', 't-demo', 'emp-003', '2026-03-10', '2026-03-10 09:00:00.000000', '2026-03-10 17:30:00.000000', 'MANUAL', NULL, CURRENT_TIMESTAMP(6)),
  ('ar-batch-004a', 't-demo', 'emp-004', '2026-03-11', '2026-03-11 09:00:00.000000', '2026-03-11 18:00:00.000000', 'MANUAL', NULL, CURRENT_TIMESTAMP(6)),
  ('ar-batch-004b', 't-demo', 'emp-004', '2026-03-14', NULL, '2026-03-14 18:00:00.000000', 'MANUAL', NULL, CURRENT_TIMESTAMP(6)),
  ('ar-batch-005a', 't-demo', 'emp-005', '2026-03-15', '2026-03-15 09:10:00.000000', '2026-03-15 18:00:00.000000', 'MANUAL', NULL, CURRENT_TIMESTAMP(6)),
  ('ar-batch-005b', 't-demo', 'emp-005', '2026-03-18', '2026-03-18 09:00:00.000000', '2026-03-18 18:00:00.000000', 'MANUAL', NULL, CURRENT_TIMESTAMP(6));

INSERT INTO attendance_anomaly (
  id, tenant_id, employee_id, work_date, type, severity, rule_hit, evidence_json, snapshot_id, created_at
) VALUES
  ('aa-batch-002a', 't-demo', 'emp-002', '2026-03-07', 'LATE', 'MEDIUM', 'rule:late_after_0905',
   '{"facts":{"checkInAt":"2026-03-07T09:25:00","threshold":"09:05"},"summary":"迟到：上班打卡晚于 09:05"}',
   'as-demo-emp002-202603', CURRENT_TIMESTAMP(6)),
  ('aa-batch-003a', 't-demo', 'emp-003', '2026-03-08', 'MISSING_CHECKOUT', 'HIGH', 'rule:missing_checkout',
   '{"facts":{"workDate":"2026-03-08"},"summary":"缺卡：未打卡下班"}',
   'as-demo-emp003-202603', CURRENT_TIMESTAMP(6)),
  ('aa-batch-003b', 't-demo', 'emp-003', '2026-03-10', 'EARLY_LEAVE', 'LOW', 'rule:early_leave_before_1800',
   '{"facts":{"checkOutAt":"2026-03-10T17:30:00","threshold":"18:00"},"summary":"早退：下班打卡早于 18:00"}',
   'as-demo-emp003-202603', CURRENT_TIMESTAMP(6)),
  ('aa-batch-004a', 't-demo', 'emp-004', '2026-03-14', 'MISSING_CHECKIN', 'HIGH', 'rule:missing_checkin',
   '{"facts":{"workDate":"2026-03-14"},"summary":"缺卡：未打卡上班"}',
   'as-demo-emp004-202603', CURRENT_TIMESTAMP(6)),
  ('aa-batch-005a', 't-demo', 'emp-005', '2026-03-15', 'LATE', 'LOW', 'rule:late_after_0905',
   '{"facts":{"checkInAt":"2026-03-15T09:10:00","threshold":"09:05"},"summary":"迟到：上班打卡晚于 09:05"}',
   'as-demo-emp005-202603', CURRENT_TIMESTAMP(6));
