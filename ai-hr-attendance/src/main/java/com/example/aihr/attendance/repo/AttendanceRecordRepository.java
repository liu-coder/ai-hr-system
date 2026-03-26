package com.example.aihr.attendance.repo;

import com.example.aihr.attendance.domain.AttendanceRecordEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecordEntity, String> {
    List<AttendanceRecordEntity> findByTenantIdAndEmployeeIdAndWorkDateBetween(
            String tenantId, String employeeId, LocalDate start, LocalDate end
    );
}

