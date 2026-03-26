package com.example.aihr.common.audit;

import java.time.Instant;
import java.util.Map;

public record AuditEvent(
        String tenantId,
        String actorUserId,
        String actorType,
        String action,
        String resourceType,
        String resourceId,
        String traceId,
        Instant occurredAt,
        Map<String, Object> attributes
) {
}

