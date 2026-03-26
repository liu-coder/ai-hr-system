package com.example.aihr.aicore.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 智能排班请求DTO
 */
public class SchedulingRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> employeeIds;
    private WorkHoursDto workHours;
    private List<String> skillRequirements;
    private int maxConsecutiveWorkingDays;
    private int minRestHoursBetweenShifts;
    
    // Getters and setters
    public LocalDate getStartDate() {
        return startDate;
    }
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    public LocalDate getEndDate() {
        return endDate;
    }
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    public List<String> getEmployeeIds() {
        return employeeIds;
    }
    public void setEmployeeIds(List<String> employeeIds) {
        this.employeeIds = employeeIds;
    }
    public WorkHoursDto getWorkHours() {
        return workHours;
    }
    public void setWorkHours(WorkHoursDto workHours) {
        this.workHours = workHours;
    }
    public List<String> getSkillRequirements() {
        return skillRequirements;
    }
    public void setSkillRequirements(List<String> skillRequirements) {
        this.skillRequirements = skillRequirements;
    }
    public int getMaxConsecutiveWorkingDays() {
        return maxConsecutiveWorkingDays;
    }
    public void setMaxConsecutiveWorkingDays(int maxConsecutiveWorkingDays) {
        this.maxConsecutiveWorkingDays = maxConsecutiveWorkingDays;
    }
    public int getMinRestHoursBetweenShifts() {
        return minRestHoursBetweenShifts;
    }
    public void setMinRestHoursBetweenShifts(int minRestHoursBetweenShifts) {
        this.minRestHoursBetweenShifts = minRestHoursBetweenShifts;
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
}
