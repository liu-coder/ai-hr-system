package com.example.aihr.aicore.repo;

import com.example.aihr.aicore.domain.ToolCallLogEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolCallLogRepository extends JpaRepository<ToolCallLogEntity, String> {

    /** 按会话取最近一次工具调用（用于统一响应中的工具调用证据） */
    Optional<ToolCallLogEntity> findFirstBySessionIdOrderByCreatedAtDesc(String sessionId);
}

