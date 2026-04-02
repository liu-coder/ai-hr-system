package com.example.aihr.aicore.web.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 工时预测请求DTO
 */
public class WorkHourPredictionRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> departmentIds;
    private List<String> employeeIds;
    private boolean includeHistoricalData;
    private int historicalMonths;
    
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
    public List<String> getDepartmentIds() {
        return departmentIds;
    }
    public void setDepartmentIds(List<String> departmentIds) {
        this.departmentIds = departmentIds;
    }
    public List<String> getEmployeeIds() {
        return employeeIds;
    }
    public void setEmployeeIds(List<String> employeeIds) {
        this.employeeIds = employeeIds;
    }
    public boolean isIncludeHistoricalData() {
        return includeHistoricalData;
    }
    public void setIncludeHistoricalData(boolean includeHistoricalData) {
        this.includeHistoricalData = includeHistoricalData;
    }
    public int getHistoricalMonths() {
        return historicalMonths;
    }
    public void setHistoricalMonths(int historicalMonths) {
        this.historicalMonths = historicalMonths;
    }
}



