package com.example.aihr.salary.web;

import com.example.aihr.salary.service.AuditService;
import com.example.aihr.salary.service.SalaryService;
import com.example.aihr.common.security.RequestContext;
import com.example.aihr.common.security.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v1/salary", produces = MediaType.APPLICATION_JSON_VALUE)
public class SalaryController {
    private final SalaryService salary;
    private final AuditService audit;
    private final ObjectMapper om = new ObjectMapper();

    public SalaryController(SalaryService salary, AuditService audit) {
        this.salary = salary;
        this.audit = audit;
    }

    /**
     * 创建一套薪酬计算策略（Salary Policy）。
     *
     * @param req 请求体
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return `{ "id": "..." }`，其中 `id` 是策略ID（形如 `sp-...`）
     */
    @PostMapping(path = "/policies", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> createPolicy(@Valid @RequestBody CreatePolicyRequest req, HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();
        String userId = ctx.userId();

        com.example.aihr.salary.domain.SalaryPolicyEntity e =
                salary.createPolicy(
                        tenantId,
                        req.name(),
                        req.version(),
                        req.effectiveFrom(),
                        req.effectiveTo(),
                        req.policyJson(),
                        userId);

        audit.write(
                tenantId,
                userId,
                "HUMAN",
                "salary.policy.create",
                "SalaryPolicy",
                e.getId(),
                http.getHeader("X-Trace-Id"),
                Map.of("name", e.getName(), "version", e.getVersion())
        );

        return Map.of("id", e.getId());
    }

    /**
     * 预览某员工某个周期的薪资结果（不改变最终发薪流程，仅生成可审计的计算快照）。
     *
     * 入参说明：
     * - `input`：员工维度的输入数据（结构由前端/调用方自定义，当前实现会序列化为 JSON）
     * - `policyId`：可选；为空时按当前“生效策略”查找
     *
     * @param req 请求体
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return 出参包含计算运行ID、策略信息、输入快照ID、结果 hash 以及明细 lines
     */
    @PostMapping(path = "/preview/employee", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> previewEmployee(@Valid @RequestBody PreviewEmployeeRequest req, HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();
        String userId = ctx.userId();

        SalaryService.PreviewResult result =
                salary.previewEmployee(tenantId, req.payPeriod(), req.employeeId(), req.input(), req.policyId());

        audit.write(
                tenantId,
                userId,
                "HUMAN",
                "salary.preview.employee",
                "SalaryCalcRun",
                result.calcRunId(),
                http.getHeader("X-Trace-Id"),
                Map.of(
                        "employeeId", req.employeeId(),
                        "payPeriod", req.payPeriod(),
                        "policyId", result.policyId(),
                        "resultHash", result.resultHash()
                )
        );

        return Map.of(
                "calcRunId", result.calcRunId(),
                "policyId", result.policyId(),
                "policyVersion", result.policyVersion(),
                "inputSnapshotId", result.inputSnapshotId(),
                "resultHash", result.resultHash(),
                "lines", result.lines().stream().map(l -> Map.of(
                        "itemCode", l.getItemCode(),
                        "itemName", l.getItemName(),
                        "amountCents", l.getAmountCents(),
                        "currency", l.getCurrency()
                )).toList()
        );
    }

    /**
     * 查询某次薪资计算结果的明细行（lines）。
     *
     * @param calcRunId 薪资计算运行ID（来自 `preview/employee` 的 `calcRunId`）
     * @return 每行包含：`id`、`employeeId`、`itemCode`、`itemName`、`amountCents`、`currency`、`detail`
     */
    @GetMapping(path = "/runs/lines")
    public List<Map<String, Object>> getRunLines(@RequestParam @NotBlank String calcRunId) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();

        return salary.getRunLines(tenantId, calcRunId).stream()
                .map(l -> {
                    Object detail = parseDetail(l.getDetailJson());
                    return Map.<String, Object>of(
                            "id", l.getId(),
                            "employeeId", l.getEmployeeId(),
                            "itemCode", l.getItemCode(),
                            "itemName", l.getItemName(),
                            "amountCents", l.getAmountCents(),
                            "currency", l.getCurrency(),
                            "detail", detail != null ? detail : Map.of()
                    );
                })
                .toList();
    }

    private Object parseDetail(String detailJson) {
        if (detailJson == null || detailJson.isBlank()) return null;
        try {
            return om.readValue(detailJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 创建策略请求体。
     */
    public record CreatePolicyRequest(
            /** 策略名称（必填） */
            @NotBlank String name,
            /** 策略版本（必填） */
            @NotBlank String version,
            /** 生效开始日期（必填） */
            @NotNull LocalDate effectiveFrom,
            /** 生效结束日期（可空：当你不填时由业务处理） */
            LocalDate effectiveTo,
            /** 策略定义 JSON 字符串（必填；当前实现原样入库） */
            @NotBlank String policyJson
    ) {}

    /**
     * 预览员工薪资请求体。
     */
    public record PreviewEmployeeRequest(
            /** 计薪周期，例如 `2026-03`（必填） */
            @NotBlank String payPeriod,
            /** 员工ID（必填） */
            @NotBlank String employeeId,
            /** 员工输入数据（可为任意 JSON 结构；当前实现会序列化成 JSON 存快照） */
            Object input,
            /** 指定策略ID（可空：为空则取当前生效策略） */
            String policyId
    ) {}
}

