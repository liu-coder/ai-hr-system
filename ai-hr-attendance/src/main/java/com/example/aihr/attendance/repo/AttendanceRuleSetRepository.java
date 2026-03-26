package com.example.aihr.attendance.repo;

import com.example.aihr.attendance.domain.AttendanceRuleSetEntity;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRuleSetRepository extends JpaRepository<AttendanceRuleSetEntity, String> {

    @Query("""
            select r from AttendanceRuleSetEntity r
            where r.tenantId = :tenantId
              and r.status = 'ACTIVE'
              and r.effectiveFrom <= :asOf
              and (r.effectiveTo is null or r.effectiveTo >= :asOf)
            order by r.effectiveFrom desc
            """)
    Optional<AttendanceRuleSetEntity> findActiveRuleSet(@Param("tenantId") String tenantId, @Param("asOf") LocalDate asOf);
}

