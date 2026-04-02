package com.example.aihr.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 基础审计服务实现
 */
public abstract class BaseAuditService implements AuditService {
    
    protected final ObjectMapper om;
    
    public BaseAuditService(ObjectMapper om) {
        this.om = om;
    }
    
    @Override
    public void write(String tenantId,
                      String actorUserId,
                      String actorType,
                      String action,
                      String resourceType,
                      String resourceId,
                      String traceId,
                      Object attributes) {
        String attributesJson = null;
        try {
            attributesJson = attributes == null ? null : om.writeValueAsString(attributes);
        } catch (Exception ex) {
            // 忽略序列化异常
        }
        
        saveAuditEvent(tenantId, actorUserId, actorType, action, resourceType, resourceId, traceId, 
                      LocalDateTime.now(ZoneOffset.UTC), attributesJson);
    }
    
    /**
     * 保存审计事件到具体的存储
     */
    protected abstract void saveAuditEvent(String tenantId,
                                          String actorUserId,
                                          String actorType,
                                          String action,
                                          String resourceType,
                                          String resourceId,
                                          String traceId,
                                          LocalDateTime occurredAt,
                                          String attributesJson);
}
