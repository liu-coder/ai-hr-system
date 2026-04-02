-- 演示：租户 t-demo，周期 2026-03，calcRunId 与前端默认占位一致便于直接查明细
INSERT INTO salary_policy (
  id, tenant_id, name, version, effective_from, effective_to, status, policy_json, created_at, created_by
) VALUES (
  'sp-demo-tdemo',
  't-demo',
  'Demo Policy',
  'v1',
  '2026-01-01',
  NULL,
  'ACTIVE',
  '{"taxRateBps":300,"socialInsuranceRateBps":1000,"housingFundRateBps":800}',
  CURRENT_TIMESTAMP(6),
  'seed'
);

INSERT INTO salary_input_snapshot (
  id, tenant_id, pay_period, snapshot_json, source_hash, created_at, created_by
) VALUES (
  'sis-demo-tdemo-202603',
  't-demo',
  '2026-03',
  '{"employeeId":"emp-001","baseSalaryCents":2000000,"overtimeCents":120000,"bonusCents":0,"deductionsCents":30000}',
  'demo-snapshot-hash',
  CURRENT_TIMESTAMP(6),
  'seed'
);

INSERT INTO salary_calc_run (
  id, tenant_id, pay_period, scope_type, scope_id, policy_id, calc_version, input_snapshot_id, result_hash, status, created_at, created_by
) VALUES (
  'scr-demo-tdemo-202603',
  't-demo',
  '2026-03',
  'EMPLOYEE',
  'emp-001',
  'sp-demo-tdemo',
  'v1',
  'sis-demo-tdemo-202603',
  'demo-result-hash',
  'SUCCESS',
  CURRENT_TIMESTAMP(6),
  'seed'
);

INSERT INTO salary_result_line (
  id, tenant_id, calc_run_id, employee_id, item_code, item_name, amount_cents, currency, detail_json, created_at
) VALUES
  ('srl-demo-001', 't-demo', 'scr-demo-tdemo-202603', 'emp-001', 'BASE', '基本工资', 2000000, 'CNY', '{"employeeId":"emp-001"}', CURRENT_TIMESTAMP(6)),
  ('srl-demo-002', 't-demo', 'scr-demo-tdemo-202603', 'emp-001', 'OVERTIME', '加班费', 120000, 'CNY', NULL, CURRENT_TIMESTAMP(6)),
  ('srl-demo-003', 't-demo', 'scr-demo-tdemo-202603', 'emp-001', 'DEDUCTIONS', '其他扣款', -30000, 'CNY', NULL, CURRENT_TIMESTAMP(6)),
  ('srl-demo-004', 't-demo', 'scr-demo-tdemo-202603', 'emp-001', 'SOCIAL', '社保', -200000, 'CNY', NULL, CURRENT_TIMESTAMP(6)),
  ('srl-demo-005', 't-demo', 'scr-demo-tdemo-202603', 'emp-001', 'HOUSING', '公积金', -160000, 'CNY', NULL, CURRENT_TIMESTAMP(6)),
  ('srl-demo-006', 't-demo', 'scr-demo-tdemo-202603', 'emp-001', 'TAX', '个税', -150000, 'CNY', NULL, CURRENT_TIMESTAMP(6)),
  ('srl-demo-007', 't-demo', 'scr-demo-tdemo-202603', 'emp-001', 'NET', '实发', 1580000, 'CNY', NULL, CURRENT_TIMESTAMP(6));
