package com.example.aihr.aicore.service;

import com.example.aihr.aicore.domain.ToolCallLogEntity;
import com.example.aihr.aicore.repo.ToolCallLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ToolCallLogger {
    private final ToolCallLogRepository repo;
    private final ObjectMapper om;

    public ToolCallLogger(ToolCallLogRepository repo, ObjectMapper om) {
        this.repo = repo;
        this.om = om;
    }

    public String start(String tenantId, String sessionId, String toolName, String traceId, Object request) {
        ToolCallLogEntity e = new ToolCallLogEntity();
        e.setId("tcl-" + UUID.randomUUID());
        e.setTenantId(tenantId);
        e.setSessionId(sessionId);
        e.setToolName(toolName);
        e.setTraceId(traceId);
        e.setStatus("STARTED");
        e.setCreatedAt(Instant.now());
        try {
            e.setRequestJson(request == null ? "{}" : om.writeValueAsString(request));
        } catch (Exception ex) {
            e.setRequestJson("{}");
        }
        repo.save(e);
        return e.getId();
    }

    public void success(String id, Object response) {
        repo.findById(id).ifPresent(e -> {
            e.setStatus("SUCCESS");
            try {
                e.setResponseJson(response == null ? "{}" : om.writeValueAsString(response));
            } catch (Exception ex) {
                e.setResponseJson("{}");
            }
            repo.save(e);
        });
    }

    public void failure(String id, String message) {
        repo.findById(id).ifPresent(e -> {
            e.setStatus("FAILED");
            try {
                e.setResponseJson(om.writeValueAsString(java.util.Map.of("error", message)));
            } catch (Exception ex) {
                e.setResponseJson("{\"error\":\"" + message.replace("\"", "'") + "\"}");
            }
            repo.save(e);
        });
    }
}

