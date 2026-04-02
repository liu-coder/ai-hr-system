package com.example.aihr.salary.web;

import com.example.aihr.salary.service.AuditService;
import com.example.aihr.salary.service.SalaryService;
import com.example.aihr.salary.service.SalaryMessageProducer;
import com.example.aihr.salary.constant.SalaryConstants;
import com.example.aihr.common.security.RequestContext;
import com.example.aihr.common.security.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 薪酬控制器
 * 处理薪酬相关的HTTP请求，包括策略管理、薪资预览和结果查询等功能
 */
@RestController
@RequestMapping(path = "/v1/salary", produces = MediaType.APPLICATION_JSON_VALUE)
public class SalaryController {
    private final SalaryService salary;
    private final AuditService audit;
    private final SalaryMessageProducer messageProducer;
    private final ObjectMapper om = new ObjectMapper();

    /**
     * 构造函数
     * 
     * @param salary 薪酬服务
     * @param audit 审计服务
     * @param messageProducer 消息生产者
     */
    public SalaryController(SalaryService salary, AuditService audit, SalaryMessageProducer messageProducer) {
        this.salary = salary;
        this.audit = audit;
        this.messageProducer = messageProducer;
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
                SalaryConstants.OPERATION_TYPE_HUMAN,
                SalaryConstants.AUDIT_OPERATION_POLICY_CREATE,
                SalaryConstants.ENTITY_TYPE_POLICY,
                e.getId(),
                http.getHeader("X-Trace-Id"),
                Map.of("name", e.getName(), "version", e.getVersion())
        );

        return Map.of("id", e.getId());
    }

    /**
     * 预览某员工某个周期的薪资结果（异步处理）。
     *
     * 入参说明：
     * - `input`：员工维度的输入数据（结构由前端/调用方自定义，当前实现会序列化为 JSON）
     * - `policyId`：可选；为空时按当前“生效策略”查找
     *
     * @param req 请求体
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return `{ "taskId": "..." }`，返回任务ID
     */
    @PostMapping(path = "/preview/employee", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> previewEmployee(@Valid @RequestBody PreviewEmployeeRequest req, HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();
        String userId = ctx.userId();

        // 生成任务ID
        String taskId = SalaryConstants.TASK_ID_PREFIX + System.currentTimeMillis() + "-" + req.employeeId();

        // 发送异步计算任务到消息队列
        messageProducer.sendSalaryPreviewTask(
                tenantId,
                req.payPeriod(),
                req.employeeId(),
                req.input(),
                req.policyId()
        );

        Map<String, Object> details = new HashMap<>();
        details.put("employeeId", req.employeeId());
        details.put("payPeriod", req.payPeriod());
        details.put("taskId", taskId);
        if (req.policyId() != null) {
            details.put("policyId", req.policyId());
        }

        audit.write(
                tenantId,
                userId,
                SalaryConstants.OPERATION_TYPE_HUMAN,
                SalaryConstants.AUDIT_OPERATION_PREVIEW_EMPLOYEE,
                SalaryConstants.ENTITY_TYPE_TASK,
                taskId,
                http.getHeader("X-Trace-Id"),
                details
        );

        return Map.of("taskId", taskId);
    }

    /**
     * 查询某次薪资计算结果的明细行（lines）。
     *
     * @param calcRunId 薪资计算运行ID（来自 `preview/employee` 的 `calcRunId`）
     * @return 每行包含：`id`、`employeeId`、`itemCode`、`itemName`、`amountCents`、`currency`、`detail`
     */
    @GetMapping(path = "/runs/lines")
    public List<Map<String, Object>> getRunLines(@RequestParam("calcRunId") @NotBlank String calcRunId) {
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

    /**
     * 解析明细JSON
     * 
     * @param detailJson 明细JSON字符串
     * @return 解析后的明细对象
     */
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

