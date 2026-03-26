package com.example.aihr.salary.service;

import com.example.aihr.salary.domain.SalaryCalcRunEntity;
import com.example.aihr.salary.domain.SalaryInputSnapshotEntity;
import com.example.aihr.salary.domain.SalaryPolicyEntity;
import com.example.aihr.salary.domain.SalaryResultLineEntity;
import com.example.aihr.common.exception.AiHrBusinessException;
import com.example.aihr.common.web.ApiError.ErrorCode;
import com.example.aihr.salary.model.SalaryPolicyDto;
import com.example.aihr.salary.repo.SalaryCalcRunRepository;
import com.example.aihr.salary.repo.SalaryInputSnapshotRepository;
import com.example.aihr.salary.repo.SalaryPolicyRepository;
import com.example.aihr.salary.repo.SalaryResultLineRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministic payroll calculation skeleton.
 *
 * Production principle: all amounts are computed deterministically from (policyVersion + inputSnapshot),
 * and every run is auditable and replayable.
 */
@Service
public class SalaryService {
    private final SalaryPolicyRepository policies;
    private final SalaryInputSnapshotRepository snapshots;
    private final SalaryCalcRunRepository runs;
    private final SalaryResultLineRepository lines;
    private final SalaryPolicyCalculator policyCalculator;
    private final ObjectMapper om;

    public SalaryService(SalaryPolicyRepository policies,
                         SalaryInputSnapshotRepository snapshots,
                         SalaryCalcRunRepository runs,
                         SalaryResultLineRepository lines,
                         SalaryPolicyCalculator policyCalculator,
                         ObjectMapper om) {
        this.policies = policies;
        this.snapshots = snapshots;
        this.runs = runs;
        this.lines = lines;
        this.policyCalculator = policyCalculator;
        this.om = om;
    }

    @Transactional
    public SalaryPolicyEntity createPolicy(String tenantId, String name, String version,
                                          LocalDate effectiveFrom, LocalDate effectiveTo,
                                          String policyJson, String createdBy) {
        SalaryPolicyEntity e = new SalaryPolicyEntity();
        e.setId("sp-" + UUID.randomUUID());
        e.setTenantId(tenantId);
        e.setName(name);
        e.setVersion(version);
        e.setEffectiveFrom(effectiveFrom);
        e.setEffectiveTo(effectiveTo);
        e.setStatus("ACTIVE");
        e.setPolicyJson(policyJson);
        e.setCreatedAt(Instant.now());
        e.setCreatedBy(createdBy);
        return policies.save(e);
    }

    /**
     * Preview payroll for a single employee, generating snapshot + calc run + result lines.
     *
     * Input example:
     * {
     *   "employeeId": "e-001",
     *   "baseSalaryCents": 2000000,
     *   "overtimeCents": 120000,
     *   "bonusCents": 0,
     *   "deductionsCents": 30000
     * }
     *
     * Policy example:
     * { "taxRateBps": 300, "socialInsuranceCents": 50000, "housingFundCents": 30000 }
     */
    @Transactional
    public PreviewResult previewEmployee(String tenantId, String payPeriod, String employeeId, Object input, String requestedPolicyId) {
        SalaryPolicyEntity policy = requestedPolicyId == null
                ? policies.findActivePolicy(tenantId, LocalDate.now()).orElseThrow(() -> new AiHrBusinessException(ErrorCode.SALARY_POLICY_NOT_FOUND))
                : policies.findById(requestedPolicyId).orElseThrow(() -> new AiHrBusinessException(ErrorCode.SALARY_POLICY_NOT_FOUND));

        String inputJson;
        try {
            inputJson = om.writeValueAsString(input);
        } catch (Exception e) {
            throw new AiHrBusinessException(ErrorCode.PARAM_INVALID_ARGUMENT, "invalid input json");
        }

        String sourceHash = sha256Hex(tenantId + "|" + payPeriod + "|" + employeeId + "|" + policy.getId() + "|" + inputJson);

        SalaryInputSnapshotEntity snap = new SalaryInputSnapshotEntity();
        snap.setId("sis-" + UUID.randomUUID());
        snap.setTenantId(tenantId);
        snap.setPayPeriod(payPeriod);
        snap.setSnapshotJson(inputJson);
        snap.setSourceHash(sourceHash);
        snap.setCreatedAt(Instant.now());
        snapshots.save(snap);

        CalcOutcome outcome = deterministicCalc(employeeId, inputJson, policy.getPolicyJson(), policyCalculator);

        SalaryCalcRunEntity run = new SalaryCalcRunEntity();
        run.setId("scr-" + UUID.randomUUID());
        run.setTenantId(tenantId);
        run.setPayPeriod(payPeriod);
        run.setScopeType("EMPLOYEE");
        run.setScopeId(employeeId);
        run.setPolicyId(policy.getId());
        run.setCalcVersion("v1");
        run.setInputSnapshotId(snap.getId());
        run.setStatus("SUCCESS");
        run.setCreatedAt(Instant.now());
        run.setResultHash(outcome.resultHash());
        runs.save(run);

        List<SalaryResultLineEntity> toSave = new ArrayList<>();
        for (Line l : outcome.lines()) {
            SalaryResultLineEntity e = new SalaryResultLineEntity();
            e.setId("srl-" + UUID.randomUUID());
            e.setTenantId(tenantId);
            e.setCalcRunId(run.getId());
            e.setEmployeeId(employeeId);
            e.setItemCode(l.itemCode());
            e.setItemName(l.itemName());
            e.setAmountCents(l.amountCents());
            e.setCurrency("CNY");
            e.setCreatedAt(Instant.now());
            try {
                e.setDetailJson(l.detail() == null ? null : om.writeValueAsString(l.detail()));
            } catch (Exception ignored) {
                e.setDetailJson(null);
            }
            toSave.add(e);
        }
        lines.saveAll(toSave);

        return new PreviewResult(run.getId(), policy.getId(), policy.getVersion(), snap.getId(), outcome.resultHash(), toSave);
    }

    public List<SalaryResultLineEntity> getRunLines(String tenantId, String calcRunId) {
        return lines.findByTenantIdAndCalcRunId(tenantId, calcRunId);
    }

    private CalcOutcome deterministicCalc(String employeeId, String inputJson, String policyJson,
                                          SalaryPolicyCalculator policyCalculator) {
        try {
            JsonNode in = om.readTree(inputJson);
            JsonNode pol = om.readTree(policyJson);

            SalaryPolicyDto policyDto = null;
            try {
                policyDto = om.readValue(policyJson, SalaryPolicyDto.class);
            } catch (Exception ignored) { }

            List<Line> configLines = policyCalculator.computeByItemDefinitions(employeeId, in, policyDto);
            if (configLines != null && !configLines.isEmpty()) {
                long net = configLines.stream().filter(l -> "NET".equals(l.itemCode())).mapToLong(Line::amountCents).findFirst().orElse(0);
                String resultHash = sha256Hex(employeeId + "|" + inputJson + "|" + policyJson + "|" + net);
                return new CalcOutcome(resultHash, configLines);
            }

            // 回退：无 itemDefinitions 时使用原有骨架计算
            long base = in.path("baseSalaryCents").asLong(0);
            long overtime = in.path("overtimeCents").asLong(0);
            long bonus = in.path("bonusCents").asLong(0);
            long deductions = in.path("deductionsCents").asLong(0);

            int taxRateBps = pol.path("taxRateBps").asInt(0); // basis points, 10000 = 100%
            long social = pol.path("socialInsuranceCents").asLong(0);
            long housing = pol.path("housingFundCents").asLong(0);

            long gross = base + overtime + bonus;
            long preTax = gross - deductions - social - housing;
            if (preTax < 0) preTax = 0;
            long tax = Math.round(preTax * (taxRateBps / 10000.0));
            long net = gross - deductions - social - housing - tax;

            List<Line> out = List.of(
                    new Line("BASE", "基本工资", base, Map.of("employeeId", employeeId)),
                    new Line("OVERTIME", "加班费", overtime, null),
                    new Line("BONUS", "奖金", bonus, null),
                    new Line("DEDUCTIONS", "其他扣款", -deductions, null),
                    new Line("SOCIAL", "社保", -social, null),
                    new Line("HOUSING", "公积金", -housing, null),
                    new Line("TAX", "个税", -tax, Map.of("taxRateBps", taxRateBps)),
                    new Line("NET", "实发", net, null)
            );

            String resultHash = sha256Hex(employeeId + "|" + inputJson + "|" + policyJson + "|" + net);
            return new CalcOutcome(resultHash, out);
        } catch (Exception e) {
            throw new AiHrBusinessException(ErrorCode.SALARY_CALC_FAILED, "calc failed: invalid json");
        }
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public record Line(String itemCode, String itemName, long amountCents, Map<String, Object> detail) {}
    private record CalcOutcome(String resultHash, List<Line> lines) {}

    public record PreviewResult(
            String calcRunId,
            String policyId,
            String policyVersion,
            String inputSnapshotId,
            String resultHash,
            List<SalaryResultLineEntity> lines
    ) {}
}

