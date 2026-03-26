package com.example.aihr.salary.repo;

import com.example.aihr.salary.domain.SalaryPolicyEntity;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalaryPolicyRepository extends JpaRepository<SalaryPolicyEntity, String> {
    @Query("""
            select p from SalaryPolicyEntity p
            where p.tenantId = :tenantId
              and p.status = 'ACTIVE'
              and p.effectiveFrom <= :asOf
              and (p.effectiveTo is null or p.effectiveTo >= :asOf)
            order by p.effectiveFrom desc
            """)
    Optional<SalaryPolicyEntity> findActivePolicy(@Param("tenantId") String tenantId, @Param("asOf") LocalDate asOf);
}

