package com.example.aihr.attendance.repo;

import com.example.aihr.attendance.domain.AttendanceAnomalyEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceAnomalyRepository extends JpaRepository<AttendanceAnomalyEntity, String> {
    List<AttendanceAnomalyEntity> findByTenantIdAndEmployeeIdAndWorkDateBetween(
            String tenantId, String employeeId, LocalDate start, LocalDate end
    );

    void deleteByTenantIdAndEmployeeIdAndWorkDateBetween(
            String tenantId, String employeeId, LocalDate start, LocalDate end
    );
}

