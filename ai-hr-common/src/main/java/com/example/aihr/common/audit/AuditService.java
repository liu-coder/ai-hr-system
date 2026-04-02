package com.example.aihr.common.audit;

/**
 * 通用审计服务接口
 */
public interface AuditService {
    
    /**
     * 写入审计事件
     * 
     * @param tenantId 租户ID
     * @param actorUserId 操作者用户ID
     * @param actorType 操作者类型
     * @param action 操作类型
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param traceId 跟踪ID
     * @param attributes 附加属性
     */
    void write(String tenantId,
               String actorUserId,
               String actorType,
               String action,
               String resourceType,
               String resourceId,
               String traceId,
               Object attributes);
}
