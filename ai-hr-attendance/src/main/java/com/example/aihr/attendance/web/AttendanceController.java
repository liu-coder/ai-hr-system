package com.example.aihr.attendance.web;

import com.example.aihr.attendance.service.AttendanceService;
import com.example.aihr.attendance.web.dto.AnomalyResponseDto;
import com.example.aihr.attendance.service.AuditService;
import com.example.aihr.common.security.RequestContext;
import com.example.aihr.common.security.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
@RequestMapping(path = "/v1/attendance", produces = MediaType.APPLICATION_JSON_VALUE)
public class AttendanceController {
    private final AttendanceService attendance;
    private final AuditService audit;

    public AttendanceController(AttendanceService attendance, AuditService audit) {
        this.attendance = attendance;
        this.audit = audit;
    }

    /**
     * 创建一套考勤规则集（Rule Set）。
     * <p>
     * 用于后续对打卡记录进行异常计算：服务会把你提供的 `ruleJson` 持久化，并标记为可用状态。
     *
     * @param req 请求体
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return `{ "id": "..." }`，其中 `id` 是规则集ID（形如 `ars-...`）
     */
    @PostMapping(path = "/rule-sets", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> createRuleSet(@Valid @RequestBody CreateRuleSetRequest req, HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();
        String userId = ctx.userId();

        // 保存规则集并返回ID
        com.example.aihr.attendance.domain.AttendanceRuleSetEntity e =
                attendance.createRuleSet(
                        tenantId,
                        req.name(),
                        req.version(),
                        req.effectiveFrom(),
                        req.effectiveTo(),
                        req.ruleJson(),
                        userId);

        audit.write(
                tenantId,
                userId,
                "HUMAN",
                "attendance.ruleset.create",
                "AttendanceRuleSet",
                e.getId(),
                http.getHeader("X-Trace-Id"),
                Map.of("name", e.getName(), "version", e.getVersion())
        );

        // 出参：规则集ID
        return Map.of("id", e.getId());
    }

    /**
     * 写入/更新一条员工的打卡记录（Upsert）。
     *
     * @param req 请求体
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return `{ "id": "..." }`，其中 `id` 是记录ID（形如 `ar-...`，通常由 tenantId+employeeId+workDate 计算得到）
     */
    @PostMapping(path = "/records", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> upsertRecord(@Valid @RequestBody UpsertRecordRequest req, HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();
        String userId = ctx.userId();

        com.example.aihr.attendance.domain.AttendanceRecordEntity e =
                attendance.upsertRecord(
                        tenantId,
                        req.employeeId(),
                        req.workDate(),
                        req.checkInAt(),
                        req.checkOutAt(),
                        req.source(),
                        req.rawPayload());

        audit.write(
                tenantId,
                userId,
                "HUMAN",
                "attendance.record.upsert",
                "AttendanceRecord",
                e.getId(),
                http.getHeader("X-Trace-Id"),
                Map.of("employeeId", req.employeeId(), "workDate", req.workDate().toString())
        );

        return Map.of("id", e.getId());
    }

    /**
     * 计算某段时间内某员工的考勤异常。
     *
     * @param req 请求体
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return `AttendanceService.ComputeResult`，包含快照ID与异常数量等信息
     */
    @PostMapping(path = "/anomalies/compute", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AttendanceService.ComputeResult compute(@Valid @RequestBody ComputeRequest req, HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();
        String userId = ctx.userId();

        AttendanceService.ComputeResult result =
                attendance.computeAnomalies(tenantId, req.employeeId(), req.start(), req.end(), req.ruleSetId());

        audit.write(
                tenantId,
                userId,
                "HUMAN",
                "attendance.anomaly.compute",
                "AttendanceSnapshot",
                result.snapshotId(),
                http.getHeader("X-Trace-Id"),
                Map.of(
                        "employeeId", req.employeeId(),
                        "start", req.start(),
                        "end", req.end(),
                        "ruleSetId", result.ruleSetId(),
                        "anomaliesCreated", result.anomaliesCreated()
                )
        );

        return result;
    }

    /**
     * 查询某员工在指定日期区间内的异常列表。
     *
     * @param employeeId 员工ID
     * @param start 区间开始日期（含）
     * @param end 区间结束日期（含）
     * @return 列表元素为 Map，字段含义见代码注释
     */
    /**
     * 查询异常列表。返回结构中 ruleHit / evidence 为结构化对象，便于 AI 再解释与前端展示。
     */
    @GetMapping(path = "/anomalies")
    public List<AnomalyResponseDto> listAnomalies(@RequestParam @NotBlank String employeeId,
                                                   @RequestParam @NotNull LocalDate start,
                                                   @RequestParam @NotNull LocalDate end) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();

        return attendance.listAnomalies(tenantId, employeeId, start, end).stream()
                .map(attendance::toAnomalyResponse)
                .toList();
    }

    /**
     * 请求体：创建规则集。
     */
    public record CreateRuleSetRequest(
            /** 规则集名称（必填） */
            @NotBlank String name,
            /** 规则集版本（必填） */
            @NotBlank String version,
            /** 规则生效开始日期（必填） */
            @NotNull LocalDate effectiveFrom,
            /** 规则生效结束日期（可空；当你不填时由业务按“无限期/当前策略”处理） */
            LocalDate effectiveTo,
            /** 规则定义的 JSON 字符串（必填；当前实现会原样入库） */
            @NotBlank String ruleJson
    ) {}

    /**
     * 请求体：写入/更新员工打卡记录。
     */
    public record UpsertRecordRequest(
            /** 员工ID（必填） */
            @NotBlank String employeeId,
            /** 工作日期（必填） */
            @NotNull LocalDate workDate,
            /** 当天首次打卡时间（必填） */
            LocalDateTime checkInAt,
            /** 当天最后一次打卡时间（必填） */
            LocalDateTime checkOutAt,
            /** 数据来源（可空，例如 MANUAL/设备/导入任务等） */
            String source,
            /** 原始载荷：用于扩展保存（可为任意 JSON/对象结构；当前会序列化成字符串） */
            Object rawPayload
    ) {}

    /**
     * 请求体：计算异常。
     */
    public record ComputeRequest(
            /** 员工ID（必填） */
            @NotBlank String employeeId,
            /** 计算区间开始日期（必填） */
            @NotNull LocalDate start,
            /** 计算区间结束日期（必填） */
            @NotNull LocalDate end,
            /** 指定使用的规则集ID（可空：为空则按“start日期附近的当前生效规则集”选择） */
            String ruleSetId
    ) {}
}

