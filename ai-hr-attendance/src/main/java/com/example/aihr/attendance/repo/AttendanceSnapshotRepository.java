package com.example.aihr.attendance.repo;

import com.example.aihr.attendance.domain.AttendanceSnapshotEntity;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceSnapshotRepository extends JpaRepository<AttendanceSnapshotEntity, String> {
    Optional<AttendanceSnapshotEntity> findFirstByTenantIdAndEmployeeIdAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(
            String tenantId, String employeeId, LocalDate start, LocalDate end
    );
}

