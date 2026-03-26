package com.example.aihr.aicore.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 排班班次DTO
 */
public class SchedulingShiftDto {
    private String employeeId;
    private LocalDate date;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> skillRequirements;
    
    // Getters and setters
    public String getEmployeeId() {
        return employeeId;
    }
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    public LocalDate getDate() {
        return date;
    }
    public void setDate(LocalDate date) {
        this.date = date;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    public List<String> getSkillRequirements() {
        return skillRequirements;
    }
    public void setSkillRequirements(List<String> skillRequirements) {
        this.skillRequirements = skillRequirements;
    }
}
