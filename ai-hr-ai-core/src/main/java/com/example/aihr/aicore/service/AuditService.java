package com.example.aihr.aicore.service;

import com.example.aihr.aicore.domain.AuditEventEntity;
import com.example.aihr.aicore.repo.AuditEventRepository;
import com.example.aihr.common.audit.BaseAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService extends BaseAuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final AuditEventRepository repo;
    private final ExecutorService executorService;

    public AuditService(AuditEventRepository repo, ObjectMapper om) {
        super(om);
        this.repo = repo;
        // 创建一个线程池用于异步处理审计事件
        this.executorService = Executors.newFixedThreadPool(5);
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
        // 异步处理审计事件，避免阻塞主业务流程
        CompletableFuture.runAsync(() -> {
            try {
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
            } catch (Exception ex) {
                // 记录审计失败的错误，但不影响主业务流程
                logger.error("Failed to save audit event: {}", ex.getMessage(), ex);
            }
        }, executorService);
    }
}





