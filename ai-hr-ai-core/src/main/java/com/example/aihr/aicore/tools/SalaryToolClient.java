package com.example.aihr.aicore.tools;

import com.example.aihr.aicore.config.ServiceEndpointsProperties;
import com.example.aihr.aicore.service.AuditService;
import com.example.aihr.aicore.service.ToolCallLogger;
import com.example.aihr.common.security.RequestContext;
import com.example.aihr.common.security.RequestContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SalaryToolClient {
    private final RestTemplate rt;
    private final ToolCallLogger toolLog;
    private final AuditService audit;
    private final ObjectMapper om;
    private final ToolInvoker toolInvoker;

    public SalaryToolClient(@LoadBalanced RestTemplate rt,
                             ToolCallLogger toolLog,
                             AuditService audit,
                             ObjectMapper om,
                             ToolInvoker toolInvoker) {
        this.rt = rt;
        this.toolLog = toolLog;
        this.audit = audit;
        this.om = om;
        this.toolInvoker = toolInvoker;
    }

    public ToolResult<Object> previewEmployee(String sessionId, String traceId, Map<String, Object> req) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String toolId = toolLog.start(ctx.tenantId(), sessionId, "salary.previewEmployee", traceId, req);
        audit.write(ctx.tenantId(), ctx.userId(), "AGENT", "tool.call", "Tool", "salary.previewEmployee", traceId,
                Map.of("toolCallId", toolId));
        String url = "http://ai-hr-salary/v1/salary/preview/employee";
        ToolResult<Object> result = toolInvoker.invoke("salary.previewEmployee", () ->
                rt.postForObject(url, new HttpEntity<>(req, headersFromCtx(ctx, traceId)), Object.class)
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

