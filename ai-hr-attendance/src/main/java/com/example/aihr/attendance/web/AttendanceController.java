package com.example.aihr.attendance.web;

import com.example.aihr.attendance.service.AttendanceService;
import com.example.aihr.attendance.web.dto.AnomalyResponseDto;
import com.example.aihr.attendance.service.AuditService;
import com.example.aihr.attendance.service.AttendanceMessageProducer;
import com.example.aihr.attendance.constant.AttendanceConstants;
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
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping(path = "/v1/attendance", produces = MediaType.APPLICATION_JSON_VALUE)
public class AttendanceController {
    private final AttendanceService attendance;
    private final AuditService audit;
    private final AttendanceMessageProducer messageProducer;

    public AttendanceController(AttendanceService attendance, AuditService audit, AttendanceMessageProducer messageProducer) {
        this.attendance = attendance;
        this.audit = audit;
        this.messageProducer = messageProducer;
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
                AttendanceConstants.OPERATION_TYPE_HUMAN,
                AttendanceConstants.AUDIT_OPERATION_RULESET_CREATE,
                AttendanceConstants.ENTITY_TYPE_RULE_SET,
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
                AttendanceConstants.OPERATION_TYPE_HUMAN,
                AttendanceConstants.AUDIT_OPERATION_RECORD_UPSERT,
                AttendanceConstants.ENTITY_TYPE_RECORD,
                e.getId(),
                http.getHeader("X-Trace-Id"),
                Map.of("employeeId", req.employeeId(), "workDate", req.workDate().toString())
        );

        return Map.of("id", e.getId());
    }

    /**
     * 计算某段时间内某员工的考勤异常（异步处理）。
     *
     * @param req 请求体
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return `{ "taskId": "..." }`，返回任务ID
     */
    @PostMapping(path = "/anomalies/compute", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> compute(@Valid @RequestBody ComputeRequest req, HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();
        String userId = ctx.userId();

        // 生成任务ID
        String taskId = AttendanceConstants.TASK_ID_PREFIX + System.currentTimeMillis() + "-" + req.employeeId();

        // 发送异步计算任务到消息队列
        messageProducer.sendAnomalyComputeTask(
                tenantId,
                req.employeeId(),
                req.start().toString(),
                req.end().toString(),
                req.ruleSetId()
        );

        audit.write(
                tenantId,
                userId,
                AttendanceConstants.OPERATION_TYPE_HUMAN,
                AttendanceConstants.AUDIT_OPERATION_ANOMALY_COMPUTE,
                AttendanceConstants.ENTITY_TYPE_TASK,
                taskId,
                http.getHeader("X-Trace-Id"),
                Map.of(
                        "employeeId", req.employeeId(),
                        "start", req.start(),
                        "end", req.end(),
                        "ruleSetId", req.ruleSetId(),
                        "taskId", taskId
                )
        );

        return Map.of("taskId", taskId);
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
    public List<AnomalyResponseDto> listAnomalies(@RequestParam("employeeId") @NotBlank String employeeId,
                                                   @RequestParam("start") @NotNull LocalDate start,
                                                   @RequestParam("end") @NotNull LocalDate end) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();

        return attendance.listAnomalies(tenantId, employeeId, start, end).stream()
                .map(attendance::toAnomalyResponse)
                .toList();
    }
    
    /**
     * 移动端打卡API。
     * @param req 移动端打卡请求
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return `{ "id": "..." }`，其中 `id` 是记录ID
     */
    @PostMapping(path = "/mobile/checkin", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> mobileCheckin(@Valid @RequestBody MobileCheckinRequest req, HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();
        String userId = ctx.userId();

        // 构建打卡记录
        LocalDateTime checkInAt = null;
        LocalDateTime checkOutAt = null;
        if ("CHECKIN".equals(req.checkType())) {
            checkInAt = req.checkTime();
        } else if ("CHECKOUT".equals(req.checkType())) {
            checkOutAt = req.checkTime();
        }

        // 构建原始载荷
        Map<String, Object> rawPayload = new HashMap<>();
        rawPayload.put("source", req.source());
        rawPayload.put("deviceId", req.deviceId());
        if ("GPS".equals(req.source())) {
            rawPayload.put("latitude", req.latitude());
            rawPayload.put("longitude", req.longitude());
        } else if ("WIFI".equals(req.source())) {
            rawPayload.put("wifiSsid", req.wifiSsid());
        }

        // 调用服务保存打卡记录
        com.example.aihr.attendance.domain.AttendanceRecordEntity e = 
                attendance.upsertRecord(
                        tenantId,
                        req.employeeId(),
                        req.workDate(),
                        checkInAt,
                        checkOutAt,
                        req.source(),
                        rawPayload
                );

        // 记录审计日志
        audit.write(
                tenantId,
                userId,
                AttendanceConstants.OPERATION_TYPE_HUMAN,
                AttendanceConstants.AUDIT_OPERATION_RECORD_UPSERT,
                AttendanceConstants.ENTITY_TYPE_RECORD,
                e.getId(),
                http.getHeader("X-Trace-Id"),
                Map.of("employeeId", req.employeeId(), "workDate", req.workDate().toString(), "checkType", req.checkType(), "source", req.source())
        );

        return Map.of("id", e.getId());
    }
    
    /**
     * 移动端考勤查询API。
     * @param req 移动端考勤查询请求
     * @return 考勤记录列表
     */
    @PostMapping(path = "/mobile/query", consumes = MediaType.APPLICATION_JSON_VALUE)
    public MobileAttendanceQueryResponse mobileQuery(@Valid @RequestBody MobileAttendanceQueryRequest req) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();

        // 调用服务查询考勤记录
        List<com.example.aihr.attendance.domain.AttendanceRecordEntity> records = 
                attendance.findByTenantIdAndEmployeeIdAndWorkDateBetween(tenantId, req.employeeId(), req.startDate(), req.endDate());

        // 转换为移动端响应格式
        List<MobileAttendanceRecordDto> mobileRecords = new ArrayList<>();
        for (var record : records) {
            String status = "正常";
            if (record.getCheckInAt() == null || record.getCheckOutAt() == null) {
                status = "缺卡";
            }
            mobileRecords.add(new MobileAttendanceRecordDto(
                    record.getWorkDate(),
                    record.getCheckInAt(),
                    record.getCheckOutAt(),
                    status
            ));
        }

        return new MobileAttendanceQueryResponse(req.employeeId(), mobileRecords);
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
    
    /**
     * 移动端打卡请求体。
     */
    public record MobileCheckinRequest(
            /** 员工ID（必填） */
            @NotBlank String employeeId,
            /** 工作日期（必填） */
            @NotNull LocalDate workDate,
            /** 打卡时间（必填） */
            @NotNull LocalDateTime checkTime,
            /** 打卡类型（必填：CHECKIN或CHECKOUT） */
            @NotBlank String checkType,
            /** 打卡来源（必填：GPS或WIFI） */
            @NotBlank String source,
            /** 纬度（GPS打卡时必填） */
            Double latitude,
            /** 经度（GPS打卡时必填） */
            Double longitude,
            /** Wi-Fi SSID（Wi-Fi打卡时必填） */
            String wifiSsid,
            /** 设备ID（必填） */
            @NotBlank String deviceId
    ) {}
    
    /**
     * 移动端考勤查询请求体。
     */
    public record MobileAttendanceQueryRequest(
            /** 员工ID（必填） */
            @NotBlank String employeeId,
            /** 查询开始日期（必填） */
            @NotNull LocalDate startDate,
            /** 查询结束日期（必填） */
            @NotNull LocalDate endDate
    ) {}
    
    /**
     * 移动端考勤查询响应体。
     */
    public record MobileAttendanceQueryResponse(
            /** 员工ID */
            String employeeId,
            /** 考勤记录列表 */
            List<MobileAttendanceRecordDto> records
    ) {}
    
    /**
     * 移动端考勤记录DTO。
     */
    public record MobileAttendanceRecordDto(
            /** 工作日期 */
            LocalDate workDate,
            /** 上班打卡时间 */
            LocalDateTime checkInAt,
            /** 下班打卡时间 */
            LocalDateTime checkOutAt,
            /** 打卡状态 */
            String status
    ) {}
}



