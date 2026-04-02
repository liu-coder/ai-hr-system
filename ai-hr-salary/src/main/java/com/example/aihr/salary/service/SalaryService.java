package com.example.aihr.salary.service;

import com.example.aihr.salary.domain.SalaryCalcRunEntity;
import com.example.aihr.salary.domain.SalaryInputSnapshotEntity;
import com.example.aihr.salary.domain.SalaryPolicyEntity;
import com.example.aihr.salary.domain.SalaryResultLineEntity;
import com.example.aihr.salary.constant.SalaryConstants;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.aihr.common.validation.InputValidator;

/**
 * Deterministic payroll calculation skeleton.
 *
 * Production principle: all amounts are computed deterministically from (policyVersion + inputSnapshot),
 * and every run is auditable and replayable.
 */
@Service
public class SalaryService {
    private static final Logger log = LoggerFactory.getLogger(SalaryService.class);
    private final SalaryPolicyRepository policies;
    private final SalaryInputSnapshotRepository snapshots;
    private final SalaryCalcRunRepository runs;
    private final SalaryResultLineRepository lines;
    private final SalaryPolicyCalculator policyCalculator;
    private final ObjectMapper om;
    private final InputValidator inputValidator;
    private final TaxCalculator taxCalculator;

    public SalaryService(SalaryPolicyRepository policies,
                         SalaryInputSnapshotRepository snapshots,
                         SalaryCalcRunRepository runs,
                         SalaryResultLineRepository lines,
                         SalaryPolicyCalculator policyCalculator,
                         ObjectMapper om,
                         InputValidator inputValidator,
                         TaxCalculator taxCalculator) {
        this.policies = policies;
        this.snapshots = snapshots;
        this.runs = runs;
        this.lines = lines;
        this.policyCalculator = policyCalculator;
        this.om = om;
        this.inputValidator = inputValidator;
        this.taxCalculator = taxCalculator;
    }

    @Transactional
    public SalaryPolicyEntity createPolicy(String tenantId, String name, String version,
                                          LocalDate effectiveFrom, LocalDate effectiveTo,
                                          String policyJson, String createdBy) {
        // 验证输入
        if (!inputValidator.isValidTenantId(tenantId)) {
            throw new com.example.aihr.common.exception.AiHrBusinessException(
                com.example.aihr.common.web.ApiError.ErrorCode.PARAM_INVALID_ARGUMENT, "Invalid tenantId");
        }
        inputValidator.validateLength(name, 100, "name");
        inputValidator.validateLength(version, 50, "version");
        inputValidator.validateLength(policyJson, 10000, "policyJson");
        inputValidator.validateLength(createdBy, 100, "createdBy");
        
        SalaryPolicyEntity e = new SalaryPolicyEntity();
        e.setId(SalaryConstants.POLICY_ID_PREFIX + UUID.randomUUID());
        e.setTenantId(tenantId);
        e.setName(name);
        e.setVersion(version);
        e.setEffectiveFrom(effectiveFrom);
        e.setEffectiveTo(effectiveTo);
        e.setStatus(SalaryConstants.STATUS_ACTIVE);
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
        // 验证输入
        if (!inputValidator.isValidTenantId(tenantId)) {
            throw new com.example.aihr.common.exception.AiHrBusinessException(
                com.example.aihr.common.web.ApiError.ErrorCode.PARAM_INVALID_ARGUMENT, "Invalid tenantId");
        }
        if (!inputValidator.isValidEmployeeId(employeeId)) {
            throw new com.example.aihr.common.exception.AiHrBusinessException(
                com.example.aihr.common.web.ApiError.ErrorCode.PARAM_INVALID_ARGUMENT, "Invalid employeeId");
        }
        inputValidator.validateLength(payPeriod, 10, "payPeriod");
        if (requestedPolicyId != null) {
            inputValidator.validateLength(requestedPolicyId, 100, "requestedPolicyId");
        }
        
        SalaryPolicyEntity policy = requestedPolicyId == null
                ? policies.findActivePolicy(tenantId, LocalDate.now()).orElseThrow(() -> new AiHrBusinessException(ErrorCode.SALARY_POLICY_NOT_FOUND))
                : policies.findById(requestedPolicyId).orElseThrow(() -> new AiHrBusinessException(ErrorCode.SALARY_POLICY_NOT_FOUND));

        String inputJson;
        try {
            inputJson = om.writeValueAsString(input);
        } catch (Exception e) {
            log.warn("Failed to serialize input: {}", e.getMessage());
            throw new AiHrBusinessException(ErrorCode.PARAM_INVALID_ARGUMENT, "invalid input json");
        }

        String sourceHash = sha256Hex(tenantId + "|" + payPeriod + "|" + employeeId + "|" + policy.getId() + "|" + inputJson);

        SalaryInputSnapshotEntity snap = new SalaryInputSnapshotEntity();
        snap.setId(SalaryConstants.INPUT_SNAPSHOT_ID_PREFIX + UUID.randomUUID());
        snap.setTenantId(tenantId);
        snap.setPayPeriod(payPeriod);
        snap.setSnapshotJson(inputJson);
        snap.setSourceHash(sourceHash);
        snap.setCreatedAt(Instant.now());
        snapshots.save(snap);

        CalcOutcome outcome = deterministicCalc(employeeId, inputJson, policy.getPolicyJson(), policyCalculator);

        SalaryCalcRunEntity run = new SalaryCalcRunEntity();
        run.setId(SalaryConstants.CALC_RUN_ID_PREFIX + UUID.randomUUID());
        run.setTenantId(tenantId);
        run.setPayPeriod(payPeriod);
        run.setScopeType(SalaryConstants.SCOPE_TYPE_EMPLOYEE);
        run.setScopeId(employeeId);
        run.setPolicyId(policy.getId());
        run.setCalcVersion(SalaryConstants.CALC_VERSION_V1);
        run.setInputSnapshotId(snap.getId());
        run.setStatus(SalaryConstants.STATUS_SUCCESS);
        run.setCreatedAt(Instant.now());
        run.setResultHash(outcome.resultHash());
        runs.save(run);

        List<SalaryResultLineEntity> resultLines = new ArrayList<>();
        for (Line line : outcome.lines()) {
            SalaryResultLineEntity resultLine = new SalaryResultLineEntity();
            resultLine.setId(SalaryConstants.RESULT_LINE_ID_PREFIX + UUID.randomUUID());
            resultLine.setTenantId(tenantId);
            resultLine.setCalcRunId(run.getId());
            resultLine.setEmployeeId(employeeId);
            resultLine.setItemCode(line.itemCode());
            resultLine.setItemName(line.itemName());
            resultLine.setAmountCents(line.amountCents());
            resultLine.setCurrency(SalaryConstants.CURRENCY_CNY);
            resultLine.setCreatedAt(Instant.now());
            try {
                resultLine.setDetailJson(line.detail() == null ? null : om.writeValueAsString(line.detail()));
            } catch (Exception ex) {
                log.warn("Failed to serialize detail json: {}", ex.getMessage());
                resultLine.setDetailJson(null);
            }
            resultLines.add(resultLine);
        }
        lines.saveAll(resultLines);

        return new PreviewResult(run.getId(), policy.getId(), policy.getVersion(), snap.getId(), outcome.resultHash(), resultLines);
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
            } catch (Exception e) {
                log.warn("Failed to parse policy dto: {}", e.getMessage());
            }

            List<Line> configLines = policyCalculator.computeByItemDefinitions(employeeId, in, policyDto);
            if (configLines != null && !configLines.isEmpty()) {
                long net = configLines.stream().filter(l -> SalaryConstants.ITEM_CODE_NET.equals(l.itemCode())).mapToLong(Line::amountCents).findFirst().orElse(0);
                String resultHash = sha256Hex(employeeId + "|" + inputJson + "|" + policyJson + "|" + net);
                return new CalcOutcome(resultHash, configLines);
            }

            // 回退：无 itemDefinitions 时使用原有骨架计算
            long base = in.path("baseSalaryCents").asLong(0);
            long overtime = in.path("overtimeCents").asLong(0);
            long bonus = in.path("bonusCents").asLong(0);
            long deductions = in.path("deductionsCents").asLong(0);

            int taxRateBps = pol.path("taxRateBps").asInt(300); // basis points, 10000 = 100%, default 3%
            int socialInsuranceRateBps = pol.path("socialInsuranceRateBps").asInt(1000); // default 10%
            int housingFundRateBps = pol.path("housingFundRateBps").asInt(800); // default 8%

            long gross = base + overtime + bonus;
            
            // 使用TaxCalculator计算税务
            long social = taxCalculator.calculateSocialInsurance(base, socialInsuranceRateBps);
            long housing = taxCalculator.calculateHousingFund(base, housingFundRateBps);
            
            // 构建员工数据
            Map<String, Object> employeeData = Map.of(
                    "childrenCount", in.path("childrenCount").asInt(0),
                    "continuingEducation", in.path("continuingEducation").asBoolean(false),
                    "housingLoan", in.path("housingLoan").asBoolean(false),
                    "housingRent", in.path("housingRent").asBoolean(false),
                    "cityType", in.path("cityType").asText("medium"),
                    "elderlySupport", in.path("elderlySupport").asBoolean(false),
                    "isOnlyChild", in.path("isOnlyChild").asBoolean(false)
            );
            
            long specialDeductions = taxCalculator.calculateSpecialDeductions(employeeData);
            long taxableIncome = taxCalculator.calculateTaxableIncome(gross, deductions, social, housing, specialDeductions);
            long tax = taxCalculator.calculateIncomeTax(taxableIncome, taxRateBps);
            long net = taxCalculator.calculateAfterTaxIncome(gross, deductions, social, housing, tax);

            List<Line> out = List.of(
                    new Line(SalaryConstants.ITEM_CODE_BASE, SalaryConstants.ITEM_NAME_BASE, base, Map.of("employeeId", employeeId)),
                    new Line(SalaryConstants.ITEM_CODE_OVERTIME, SalaryConstants.ITEM_NAME_OVERTIME, overtime, null),
                    new Line(SalaryConstants.ITEM_CODE_BONUS, SalaryConstants.ITEM_NAME_BONUS, bonus, null),
                    new Line(SalaryConstants.ITEM_CODE_DEDUCTIONS, SalaryConstants.ITEM_NAME_DEDUCTIONS, -deductions, null),
                    new Line(SalaryConstants.ITEM_CODE_SOCIAL, SalaryConstants.ITEM_NAME_SOCIAL, -social, null),
                    new Line(SalaryConstants.ITEM_CODE_HOUSING, SalaryConstants.ITEM_NAME_HOUSING, -housing, null),
                    new Line("SPECIAL_DEDUCTIONS", "专项扣除", -specialDeductions, null),
                    new Line(SalaryConstants.ITEM_CODE_TAX, SalaryConstants.ITEM_NAME_TAX, -tax, Map.of("taxRateBps", taxRateBps, "taxableIncome", taxableIncome)),
                    new Line(SalaryConstants.ITEM_CODE_NET, SalaryConstants.ITEM_NAME_NET, net, null)
            );

            String resultHash = sha256Hex(employeeId + "|" + inputJson + "|" + policyJson + "|" + net);
            return new CalcOutcome(resultHash, out);
        } catch (Exception e) {
            log.error("Salary calculation failed: {}", e.getMessage(), e);
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

