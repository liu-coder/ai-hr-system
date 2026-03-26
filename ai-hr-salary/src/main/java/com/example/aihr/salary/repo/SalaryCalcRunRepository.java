package com.example.aihr.salary.repo;

import com.example.aihr.salary.domain.SalaryCalcRunEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryCalcRunRepository extends JpaRepository<SalaryCalcRunEntity, String> {
    Optional<SalaryCalcRunEntity> findFirstByTenantIdAndPayPeriodAndScopeTypeAndScopeIdOrderByCreatedAtDesc(
            String tenantId, String payPeriod, String scopeType, String scopeId
    );
}

