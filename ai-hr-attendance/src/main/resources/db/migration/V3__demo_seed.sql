-- 演示租户 t-demo、员工 emp-001，日期与前端默认 2026-03 对齐
INSERT INTO attendance_rule_set (
  id, tenant_id, name, version, effective_from, effective_to, status, rule_json, created_at, created_by
) VALUES (
  'ars-demo-tdemo-v1',
  't-demo',
  'Demo Rules',
  'v1',
  '2026-01-01',
  NULL,
  'ACTIVE',
  '[]',
  CURRENT_TIMESTAMP(6),
  'seed'
);

INSERT INTO attendance_snapshot (
  id, tenant_id, employee_id, period_start, period_end, rule_set_id, source_hash, snapshot_json, created_at, created_by
) VALUES (
  'as-demo-tdemo-202603',
  't-demo',
  'emp-001',
  '2026-03-01',
  '2026-03-31',
  'ars-demo-tdemo-v1',
  'demo-seed-hash',
  '{}',
  CURRENT_TIMESTAMP(6),
  'seed'
);

INSERT INTO attendance_anomaly (
  id, tenant_id, employee_id, work_date, type, severity, rule_hit, evidence_json, snapshot_id, created_at
) VALUES (
  'aa-demo-tdemo-001',
  't-demo',
  'emp-001',
  '2026-03-05',
  'LATE',
  'MEDIUM',
  'rule:late_after_0905',
  '{"facts":{"checkInAt":"2026-03-05T09:20:00","threshold":"09:05"},"summary":"迟到：上班打卡晚于 09:05"}',
  'as-demo-tdemo-202603',
  CURRENT_TIMESTAMP(6)
);

INSERT INTO attendance_anomaly (
  id, tenant_id, employee_id, work_date, type, severity, rule_hit, evidence_json, snapshot_id, created_at
) VALUES (
  'aa-demo-tdemo-002',
  't-demo',
  'emp-001',
  '2026-03-12',
  'MISSING_CHECKOUT',
  'HIGH',
  'rule:missing_checkout',
  '{"facts":{"workDate":"2026-03-12"},"summary":"缺卡：未打卡下班"}',
  'as-demo-tdemo-202603',
  CURRENT_TIMESTAMP(6)
);
