package com.example.aihr.aicore.repo;

import com.example.aihr.aicore.domain.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {
}

