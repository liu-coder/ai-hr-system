package com.example.aihr.salary.service;

import com.example.aihr.salary.domain.AuditEventEntity;
import com.example.aihr.salary.repo.AuditEventRepository;
import com.example.aihr.common.audit.BaseAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * 薪酬审计服务
 * 负责记录薪酬相关的操作审计日志
 */
@Service
public class AuditService extends BaseAuditService {
    private final AuditEventRepository repo;

    /**
     * 构造函数
     * 
     * @param repo 审计事件仓库
     * @param om 对象映射器
     */
    public AuditService(AuditEventRepository repo, ObjectMapper om) {
        super(om);
        this.repo = repo;
    }

    /**
     * 保存审计事件
     * 
     * @param tenantId 租户ID
     * @param actorUserId 操作用户ID
     * @param actorType 操作者类型
     * @param action 操作类型
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param traceId 跟踪ID
     * @param occurredAt 发生时间
     * @param attributesJson 属性JSON
     */
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

