package com.example.aihr.aicore.web;

import com.example.aihr.aicore.rag.PolicyRagService;
import com.example.aihr.aicore.service.AttendanceAIService;
import com.example.aihr.aicore.service.SalaryAIService;
import com.example.aihr.aicore.web.dto.ChatResponseDto;
import com.example.aihr.aicore.web.dto.SchedulingRequestDto;
import com.example.aihr.aicore.web.dto.SchedulingResponseDto;
import com.example.aihr.aicore.web.dto.AttendanceAnomalyDetectionRequestDto;
import com.example.aihr.aicore.web.dto.AttendanceAnomalyDetectionResponseDto;
import com.example.aihr.aicore.web.dto.LeaveRequestDto;
import com.example.aihr.aicore.web.dto.LeaveApprovalResponseDto;
import com.example.aihr.aicore.web.dto.WorkHourPredictionRequestDto;
import com.example.aihr.aicore.web.dto.WorkHourPredictionResponseDto;
import com.example.aihr.aicore.web.dto.SalaryAdjustmentRequestDto;
import com.example.aihr.aicore.web.dto.SalaryAdjustmentResponseDto;
import com.example.aihr.aicore.web.dto.TaxOptimizationRequestDto;
import com.example.aihr.aicore.web.dto.TaxOptimizationResponseDto;
import com.example.aihr.aicore.web.dto.SalaryPredictionRequestDto;
import com.example.aihr.aicore.web.dto.SalaryPredictionResponseDto;
import com.example.aihr.aicore.web.dto.SalaryAnomalyDetectionRequestDto;
import com.example.aihr.aicore.web.dto.SalaryAnomalyDetectionResponseDto;
import com.example.aihr.common.exception.AiHrAuthException;
import com.example.aihr.common.exception.AiHrBusinessException;
import com.example.aihr.common.security.RequestContext;
import com.example.aihr.common.security.RequestContextHolder;
import com.example.aihr.common.security.TenantContext;
import com.example.aihr.common.security.annotation.RequireRoles;
import com.example.aihr.common.web.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI控制器
 * 处理AI相关的HTTP请求，包括聊天、策略RAG、考勤AI和薪酬AI等功能
 */
@RestController
@RequestMapping(path = "/v1/ai", produces = MediaType.APPLICATION_JSON_VALUE)
public class AiController {
    private final ChatApplicationService chatApplicationService;
    private final PolicyRagService rag;
    private final AttendanceAIService attendanceAIService;
    private final SalaryAIService salaryAIService;

    /**
     * 构造函数
     * 
     * @param chatApplicationService 聊天应用服务
     * @param rag 策略RAG服务
     * @param attendanceAIService 考勤AI服务
     * @param salaryAIService 薪酬AI服务
     */
    public AiController(ChatApplicationService chatApplicationService, PolicyRagService rag, AttendanceAIService attendanceAIService, SalaryAIService salaryAIService) {
        this.chatApplicationService = chatApplicationService;
        this.rag = rag;
        this.attendanceAIService = attendanceAIService;
        this.salaryAIService = salaryAIService;
    }

    /**
     * 聊天接口
     * 
     * @param req 聊天请求
     * @param http HTTP请求对象，用于获取X-Trace-Id
     * @return 聊天响应
     */
    @PostMapping(path = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    @RequireRoles({"USER", "HR", "ADMIN"})
    public ChatResponseDto chat(@Valid @RequestBody ChatRequest req, HttpServletRequest http) {
        return chatApplicationService.handleChat(req.sessionId(), req.message(), req.toolArgs(), http.getHeader("X-Trace-Id"));
    }

    /**
     * 策略文档导入接口
     * 
     * @param req 导入请求
     * @return 包含文档ID的Map
     */
    @PostMapping(path = "/policy/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
    @RequireRoles({"ADMIN"})
    public Map<String, Object> ingest(@Valid @RequestBody IngestRequest req) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String docId = rag.ingest(
                TenantContext.getTenantId(),
                req.docType(),
                req.title(),
                req.version(),
                req.securityLevel(),
                req.effectiveFrom(),
                req.effectiveTo(),
                req.sourceUri(),
                ctx.userId(),
                req.chunks()
        );
        return Map.of("documentId", docId);
    }

    // 考勤AI接口
    /**
     * 排班优化接口
     * 
     * @param req 排班请求
     * @return 排班响应
     */
    @PostMapping(path = "/attendance/scheduling", consumes = MediaType.APPLICATION_JSON_VALUE)
    @RequireRoles({"HR", "ADMIN"})
    public SchedulingResponseDto optimizeScheduling(@Valid @RequestBody SchedulingRequestDto req) {
        return attendanceAIService.optimizeScheduling(TenantContext.getTenantId(), req);
    }

    /**
     * 考勤异常检测接口
     * 
     * @param req 异常检测请求
     * @return 异常检测响应
     */
    @PostMapping(path = "/attendance/anomalies/detect", consumes = MediaType.APPLICATION_JSON_VALUE)
    @RequireRoles({"HR", "ADMIN"})
    public AttendanceAnomalyDetectionResponseDto detectAttendanceAnomalies(@Valid @RequestBody AttendanceAnomalyDetectionRequestDto req) {
        return attendanceAIService.detectAnomalies(TenantContext.getTenantId(), req);
    }

    /**
     * 请假审批接口
     * 
     * @param req 请假请求
     * @return 请假审批响应
     */
    @PostMapping(path = "/attendance/leave/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    @RequireRoles({"HR", "ADMIN"})
    public LeaveApprovalResponseDto approveLeave(@Valid @RequestBody LeaveRequestDto req) {
        return attendanceAIService.approveLeave(TenantContext.getTenantId(), req);
    }

    /**
     * 工时预测接口
     * 
     * @param req 工时预测请求
     * @return 工时预测响应
     */
    @PostMapping(path = "/attendance/workhours/predict", consumes = MediaType.APPLICATION_JSON_VALUE)
    @RequireRoles({"HR", "ADMIN"})
    public WorkHourPredictionResponseDto predictWorkHours(@Valid @RequestBody WorkHourPredictionRequestDto req) {
        return attendanceAIService.predictWorkHours(TenantContext.getTenantId(), req);
    }

    // 薪酬AI接口
    /**
     * 薪资调整建议接口
     * 
     * @param req 薪资调整请求
     * @return 薪资调整响应
     */
    @PostMapping(path = "/salary/adjustment/suggest", consumes = MediaType.APPLICATION_JSON_VALUE)
    @RequireRoles({"HR", "ADMIN"})
    public SalaryAdjustmentResponseDto suggestSalaryAdjustment(@Valid @RequestBody SalaryAdjustmentRequestDto req) {
        return salaryAIService.suggestSalaryAdjustment(TenantContext.getTenantId(), req);
    }

    /**
     * 税务优化接口
     * 
     * @param req 税务优化请求
     * @return 税务优化响应
     */
    @PostMapping(path = "/salary/tax/optimize", consumes = MediaType.APPLICATION_JSON_VALUE)
    @RequireRoles({"HR", "ADMIN"})
    public TaxOptimizationResponseDto optimizeTax(@Valid @RequestBody TaxOptimizationRequestDto req) {
        return salaryAIService.optimizeTax(TenantContext.getTenantId(), req);
    }

    /**
     * 薪资预测接口
     * 
     * @param req 薪资预测请求
     * @return 薪资预测响应
     */
    @PostMapping(path = "/salary/prediction", consumes = MediaType.APPLICATION_JSON_VALUE)
    @RequireRoles({"HR", "ADMIN"})
    public SalaryPredictionResponseDto predictSalary(@Valid @RequestBody SalaryPredictionRequestDto req) {
        return salaryAIService.predictSalary(TenantContext.getTenantId(), req);
    }

    /**
     * 薪资异常检测接口
     * 
     * @param req 薪资异常检测请求
     * @return 薪资异常检测响应
     */
    @PostMapping(path = "/salary/anomalies/detect", consumes = MediaType.APPLICATION_JSON_VALUE)
    @RequireRoles({"HR", "ADMIN"})
    public SalaryAnomalyDetectionResponseDto detectSalaryAnomalies(@Valid @RequestBody SalaryAnomalyDetectionRequestDto req) {
        return salaryAIService.detectSalaryAnomalies(TenantContext.getTenantId(), req);
    }

    /**
     * 聊天请求
     */
    public record ChatRequest(
            /** 会话ID */
            String sessionId,
            /** 消息内容（必填） */
            @NotBlank String message,
            /** 工具参数 */
            Map<String, Object> toolArgs
    ) {}

    /**
     * 策略文档导入请求
     */
    public record IngestRequest(
            /** 文档类型（必填） */
            @NotBlank String docType,
            /** 文档标题（必填） */
            @NotBlank String title,
            /** 文档版本（必填） */
            @NotBlank String version,
            /** 安全级别（必填） */
            @NotBlank String securityLevel,
            /** 生效开始日期（必填） */
            @NotNull LocalDate effectiveFrom,
            /** 生效结束日期 */
            LocalDate effectiveTo,
            /** 源URI */
            String sourceUri,
            /** 文档 chunks（必填） */
            @NotNull List<String> chunks
    ) {}

    /**
     * 统一异常处理
     * 
     * @param ex 异常
     * @param request HTTP请求
     * @return 统一的错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception ex, HttpServletRequest request) {
        ApiError error = new ApiError(
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "SYS",
                "服务器内部错误",
                ex.getMessage(),
                java.time.Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 业务异常处理
     * 
     * @param ex 业务异常
     * @param request HTTP请求
     * @return 业务错误响应
     */
    @ExceptionHandler(com.example.aihr.common.exception.AiHrBusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(com.example.aihr.common.exception.AiHrBusinessException ex, HttpServletRequest request) {
        ApiError error = new ApiError(
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                "SYS",
                "业务错误",
                ex.getMessage(),
                java.time.Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * 认证异常处理
     * 
     * @param ex 认证异常
     * @param request HTTP请求
     * @return 认证错误响应
     */
    @ExceptionHandler(com.example.aihr.common.exception.AiHrAuthException.class)
    public ResponseEntity<ApiError> handleAuthException(com.example.aihr.common.exception.AiHrAuthException ex, HttpServletRequest request) {
        ApiError error = new ApiError(
                String.valueOf(HttpStatus.UNAUTHORIZED.value()),
                "AUTH",
                "认证错误",
                ex.getMessage(),
                java.time.Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }
}




