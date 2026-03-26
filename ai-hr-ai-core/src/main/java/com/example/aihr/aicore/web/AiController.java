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
import com.example.aihr.common.security.RequestContext;
import com.example.aihr.common.security.RequestContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v1/ai", produces = MediaType.APPLICATION_JSON_VALUE)
public class AiController {
    private final ChatApplicationService chatApplicationService;
    private final PolicyRagService rag;
    private final AttendanceAIService attendanceAIService;
    private final SalaryAIService salaryAIService;

    public AiController(ChatApplicationService chatApplicationService, PolicyRagService rag, AttendanceAIService attendanceAIService, SalaryAIService salaryAIService) {
        this.chatApplicationService = chatApplicationService;
        this.rag = rag;
        this.attendanceAIService = attendanceAIService;
        this.salaryAIService = salaryAIService;
    }

    @PostMapping(path = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponseDto chat(@Valid @RequestBody ChatRequest req, jakarta.servlet.http.HttpServletRequest http) {
        return chatApplicationService.handleChat(req.sessionId(), req.message(), req.toolArgs(), http.getHeader("X-Trace-Id"));
    }

    @PostMapping(path = "/policy/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> ingest(@Valid @RequestBody IngestRequest req) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String docId = rag.ingest(
                ctx.tenantId(),
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
    @PostMapping(path = "/attendance/scheduling", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SchedulingResponseDto optimizeScheduling(@Valid @RequestBody SchedulingRequestDto req) {
        return attendanceAIService.optimizeScheduling(req);
    }

    @PostMapping(path = "/attendance/anomalies/detect", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AttendanceAnomalyDetectionResponseDto detectAttendanceAnomalies(@Valid @RequestBody AttendanceAnomalyDetectionRequestDto req) {
        return attendanceAIService.detectAnomalies(req);
    }

    @PostMapping(path = "/attendance/leave/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LeaveApprovalResponseDto approveLeave(@Valid @RequestBody LeaveRequestDto req) {
        return attendanceAIService.approveLeave(req);
    }

    @PostMapping(path = "/attendance/workhours/predict", consumes = MediaType.APPLICATION_JSON_VALUE)
    public WorkHourPredictionResponseDto predictWorkHours(@Valid @RequestBody WorkHourPredictionRequestDto req) {
        return attendanceAIService.predictWorkHours(req);
    }

    // 薪酬AI接口
    @PostMapping(path = "/salary/adjustment/suggest", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SalaryAdjustmentResponseDto suggestSalaryAdjustment(@Valid @RequestBody SalaryAdjustmentRequestDto req) {
        return salaryAIService.suggestSalaryAdjustment(req);
    }

    @PostMapping(path = "/salary/tax/optimize", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TaxOptimizationResponseDto optimizeTax(@Valid @RequestBody TaxOptimizationRequestDto req) {
        return salaryAIService.optimizeTax(req);
    }

    @PostMapping(path = "/salary/prediction", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SalaryPredictionResponseDto predictSalary(@Valid @RequestBody SalaryPredictionRequestDto req) {
        return salaryAIService.predictSalary(req);
    }

    @PostMapping(path = "/salary/anomalies/detect", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SalaryAnomalyDetectionResponseDto detectSalaryAnomalies(@Valid @RequestBody SalaryAnomalyDetectionRequestDto req) {
        return salaryAIService.detectSalaryAnomalies(req);
    }

    public record ChatRequest(
            String sessionId,
            @NotBlank String message,
            Map<String, Object> toolArgs
    ) {}

    public record IngestRequest(
            @NotBlank String docType,
            @NotBlank String title,
            @NotBlank String version,
            @NotBlank String securityLevel,
            @NotNull LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String sourceUri,
            @NotNull List<String> chunks
    ) {}
}
