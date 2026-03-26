package com.example.aihr.salary.repo;

import com.example.aihr.salary.domain.SalaryResultLineEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryResultLineRepository extends JpaRepository<SalaryResultLineEntity, String> {
    List<SalaryResultLineEntity> findByTenantIdAndCalcRunId(String tenantId, String calcRunId);
    List<SalaryResultLineEntity> findByTenantIdAndEmployeeIdAndCalcRunId(String tenantId, String employeeId, String calcRunId);
}

