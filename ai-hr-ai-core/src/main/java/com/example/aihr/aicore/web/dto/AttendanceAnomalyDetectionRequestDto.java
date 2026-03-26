package com.example.aihr.aicore.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 异常打卡检测请求DTO
 */
public class AttendanceAnomalyDetectionRequestDto {
    private List<AttendanceRecordDto> attendanceRecords;
    private WorkHoursDto workHours;
    private double lateThresholdMinutes;
    private double earlyLeaveThresholdMinutes;
    private LocationDto officeLocation;
    
    // Getters and setters
    public List<AttendanceRecordDto> getAttendanceRecords() {
        return attendanceRecords;
    }
    public void setAttendanceRecords(List<AttendanceRecordDto> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }
    public WorkHoursDto getWorkHours() {
        return workHours;
    }
    public void setWorkHours(WorkHoursDto workHours) {
        this.workHours = workHours;
    }
    public double getLateThresholdMinutes() {
        return lateThresholdMinutes;
    }
    public void setLateThresholdMinutes(double lateThresholdMinutes) {
        this.lateThresholdMinutes = lateThresholdMinutes;
    }
    public double getEarlyLeaveThresholdMinutes() {
        return earlyLeaveThresholdMinutes;
    }
    public void setEarlyLeaveThresholdMinutes(double earlyLeaveThresholdMinutes) {
        this.earlyLeaveThresholdMinutes = earlyLeaveThresholdMinutes;
    }
    public LocationDto getOfficeLocation() {
        return officeLocation;
    }
    public void setOfficeLocation(LocationDto officeLocation) {
        this.officeLocation = officeLocation;
    }
    
    /**
     * 打卡记录DTO
     */
    public static class AttendanceRecordDto {
        private String employeeId;
        private LocalDate workDate;
        private LocalDateTime checkInAt;
        private LocalDateTime checkOutAt;
        private LocationDto location;
        private String deviceId;
        private String ipAddress;
        
        // Getters and setters
        public String getEmployeeId() {
            return employeeId;
        }
        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }
        public LocalDate getWorkDate() {
            return workDate;
        }
        public void setWorkDate(LocalDate workDate) {
            this.workDate = workDate;
        }
        public LocalDateTime getCheckInAt() {
            return checkInAt;
        }
        public void setCheckInAt(LocalDateTime checkInAt) {
            this.checkInAt = checkInAt;
        }
        public LocalDateTime getCheckOutAt() {
            return checkOutAt;
        }
        public void setCheckOutAt(LocalDateTime checkOutAt) {
            this.checkOutAt = checkOutAt;
        }
        public LocationDto getLocation() {
            return location;
        }
        public void setLocation(LocationDto location) {
            this.location = location;
        }
        public String getDeviceId() {
            return deviceId;
        }
        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }
        public String getIpAddress() {
            return ipAddress;
        }
        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }
    }
    
    /**
     * 工作时间DTO
     */
    public static class WorkHoursDto {
        private LocalTime start;
        private LocalTime end;
        
        // Getters and setters
        public LocalTime getStart() {
            return start;
        }
        public void setStart(LocalTime start) {
            this.start = start;
        }
        public LocalTime getEnd() {
            return end;
        }
        public void setEnd(LocalTime end) {
            this.end = end;
        }
    }
    
    /**
     * 地点DTO
     */
    public static class LocationDto {
        private double latitude;
        private double longitude;
        private String address;
        
        public double getLatitude() {
            return latitude;
        }
        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }
        public double getLongitude() {
            return longitude;
        }
        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
        public String getAddress() {
            return address;
        }
        public void setAddress(String address) {
            this.address = address;
        }
    }
}
