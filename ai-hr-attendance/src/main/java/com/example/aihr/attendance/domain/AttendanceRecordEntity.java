package com.example.aihr.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_record", indexes = {
    @Index(name = "idx_tenant_employee_workdate", columnList = "tenant_id, employee_id, work_date")
})
public class AttendanceRecordEntity {
    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "check_in_at")
    private LocalDateTime checkInAt;

    @Column(name = "check_out_at")
    private LocalDateTime checkOutAt;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "raw_payload", columnDefinition = "json")
    private String rawPayload;

    @Column(name = "check_in_location")
    private String checkInLocation;

    @Column(name = "check_out_location")
    private String checkOutLocation;

    @Column(name = "check_in_location_valid")
    private boolean checkInLocationValid;

    @Column(name = "check_out_location_valid")
    private boolean checkOutLocationValid;

    @Column(name = "check_in_device_type")
    private String checkInDeviceType;

    @Column(name = "check_out_device_type")
    private String checkOutDeviceType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public LocalDateTime getCheckInAt() { return checkInAt; }
    public void setCheckInAt(LocalDateTime checkInAt) { this.checkInAt = checkInAt; }
    public LocalDateTime getCheckOutAt() { return checkOutAt; }
    public void setCheckOutAt(LocalDateTime checkOutAt) { this.checkOutAt = checkOutAt; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }
    public String getCheckInLocation() { return checkInLocation; }
    public void setCheckInLocation(String checkInLocation) { this.checkInLocation = checkInLocation; }
    public String getCheckOutLocation() { return checkOutLocation; }
    public void setCheckOutLocation(String checkOutLocation) { this.checkOutLocation = checkOutLocation; }
    public boolean isCheckInLocationValid() { return checkInLocationValid; }
    public void setCheckInLocationValid(boolean checkInLocationValid) { this.checkInLocationValid = checkInLocationValid; }
    public boolean isCheckOutLocationValid() { return checkOutLocationValid; }
    public void setCheckOutLocationValid(boolean checkOutLocationValid) { this.checkOutLocationValid = checkOutLocationValid; }
    public String getCheckInDeviceType() { return checkInDeviceType; }
    public void setCheckInDeviceType(String checkInDeviceType) { this.checkInDeviceType = checkInDeviceType; }
    public String getCheckOutDeviceType() { return checkOutDeviceType; }
    public void setCheckOutDeviceType(String checkOutDeviceType) { this.checkOutDeviceType = checkOutDeviceType; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

