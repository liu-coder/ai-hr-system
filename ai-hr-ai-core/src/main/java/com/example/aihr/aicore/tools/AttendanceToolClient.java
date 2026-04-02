package com.example.aihr.aicore.tools;


import com.example.aihr.aicore.service.AuditService;
import com.example.aihr.aicore.service.ToolCallLogger;
import com.example.aihr.aicore.tools.ToolInvoker;
import com.example.aihr.aicore.tools.ToolResult;
import com.example.aihr.common.security.RequestContext;
import com.example.aihr.common.security.RequestContextHolder;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

@Component
public class AttendanceToolClient {
    private static final Logger log = LoggerFactory.getLogger(AttendanceToolClient.class);
    private final RestTemplate rt;

    private final ToolCallLogger toolLog;
    private final AuditService audit;
    private final ObjectMapper om;
    private final ToolInvoker toolInvoker;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public AttendanceToolClient(@LoadBalanced RestTemplate rt,
                                 ToolCallLogger toolLog,
                                 AuditService audit,
                                 ObjectMapper om,
                                 ToolInvoker toolInvoker,
                                 CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.rt = rt;
        this.toolLog = toolLog;
        this.audit = audit;
        this.om = om;
        this.toolInvoker = toolInvoker;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public ToolResult<Object> computeAnomalies(String sessionId, String traceId, Map<String, Object> req) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String toolId = toolLog.start(ctx.tenantId(), sessionId, "attendance.computeAnomalies", traceId, req);
        audit.write(ctx.tenantId(), ctx.userId(), "AGENT", "tool.call", "Tool", "attendance.computeAnomalies", traceId,
                Map.of("toolCallId", toolId));
        String url = "http://ai-hr-attendance/v1/attendance/anomalies/compute";
        
        // P1: 使用熔断器包装调用
        CircuitBreaker cb = circuitBreakerFactory.create("attendanceTool");
        ToolResult<Object> result = cb.run(
            () -> toolInvoker.invoke("attendance.computeAnomalies", () -> rt.postForObject(
                    url,
                    new HttpEntity<>(req, headersFromCtx(ctx, traceId)),
                    Object.class
            )),
            throwable -> {
                log.warn("熔断器触发，attendance.computeAnomalies 失败：{}", throwable.getMessage());
                return ToolResult.failed("熔断器保护：" + throwable.getMessage());
            }
        );
        
        if (result.success()) toolLog.success(toolId, result.data());
        else toolLog.failure(toolId, result.reason());
        return result;
    }

    public ToolResult<Object> listAnomalies(String sessionId, String traceId, Map<String, Object> params) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String toolId = toolLog.start(ctx.tenantId(), sessionId, "attendance.listAnomalies", traceId, params);
        String url = "http://ai-hr-attendance/v1/attendance/anomalies"
                + "?employeeId=" + params.get("employeeId")
                + "&start=" + params.get("start")
                + "&end=" + params.get("end");

        // P1: 使用熔断器包装调用
        CircuitBreaker cb = circuitBreakerFactory.create("attendanceTool");
        ToolResult<Object> result = cb.run(
            () -> toolInvoker.invoke("attendance.listAnomalies", () -> rt.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headersFromCtx(ctx, traceId)),
                    Object.class
            ).getBody()),
            throwable -> {
                log.warn("熔断器触发，attendance.listAnomalies 失败：{}", throwable.getMessage());
                return ToolResult.failed("熔断器保护：" + throwable.getMessage());
            }
        );
        
        if (result.success()) toolLog.success(toolId, result.data());
        else toolLog.failure(toolId, result.reason());
        return result;
    }

    private HttpHeaders headersFromCtx(RequestContext ctx, String traceId) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.add("X-Tenant-Id", ctx.tenantId());
        h.add("X-User-Id", ctx.userId());
        h.add("X-Roles", String.join(",", ctx.roles()));
        try {
            h.add("X-Abac", java.util.Base64.getEncoder().encodeToString(om.writeValueAsBytes(ctx.abac())));
        } catch (Exception ignored) {
            h.add("X-Abac", "");
        }
        if (traceId != null) h.add("X-Trace-Id", traceId);
        return h;
    }
}




