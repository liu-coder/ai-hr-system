package com.example.aihr.attendance.service;

import com.example.aihr.attendance.domain.AuditEventEntity;
import com.example.aihr.attendance.repo.AuditEventRepository;
import com.example.aihr.common.audit.BaseAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AuditService extends BaseAuditService {
    private final AuditEventRepository repo;

    public AuditService(AuditEventRepository repo, ObjectMapper om) {
        super(om);
        this.repo = repo;
    }

    @Override
    protected void saveAuditEvent(String tenantId,
                                 String actorUserId,
                                 String actorType,
                                 String action,
                                 String resourceType,
                                 String resourceId,
                                 String traceId,
                                 LocalDateTime occurredAt,
                                 String attributesJson) {
        AuditEventEntity e = new AuditEventEntity();
        e.setTenantId(tenantId);
        e.setActorUserId(actorUserId);
        e.setActorType(actorType);
        e.setAction(action);
        e.setResourceType(resourceType);
        e.setResourceId(resourceId);
        e.setTraceId(traceId);
        e.setOccurredAt(occurredAt);
        e.setAttributes(attributesJson);
        repo.save(e);
    }
}

