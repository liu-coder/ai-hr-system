package com.example.aihr.salary.repo;

import com.example.aihr.salary.domain.SalaryInputSnapshotEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryInputSnapshotRepository extends JpaRepository<SalaryInputSnapshotEntity, String> {
    Optional<SalaryInputSnapshotEntity> findFirstByTenantIdAndPayPeriodOrderByCreatedAtDesc(String tenantId, String payPeriod);
}

