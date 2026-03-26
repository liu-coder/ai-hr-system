package com.example.aihr.attendance.service;

import com.example.aihr.attendance.domain.AuditEventEntity;
import com.example.aihr.attendance.repo.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditEventRepository repo;
    private final ObjectMapper om;

    public AuditService(AuditEventRepository repo, ObjectMapper om) {
        this.repo = repo;
        this.om = om;
    }

    public void write(String tenantId,
                      String actorUserId,
                      String actorType,
                      String action,
                      String resourceType,
                      String resourceId,
                      String traceId,
                      Object attributes) {
        AuditEventEntity e = new AuditEventEntity();
        e.setTenantId(tenantId);
        e.setActorUserId(actorUserId);
        e.setActorType(actorType);
        e.setAction(action);
        e.setResourceType(resourceType);
        e.setResourceId(resourceId);
        e.setTraceId(traceId);
        e.setOccurredAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            e.setAttributes(attributes == null ? null : om.writeValueAsString(attributes));
        } catch (Exception ex) {
            e.setAttributes(null);
        }
        repo.save(e);
    }
}

