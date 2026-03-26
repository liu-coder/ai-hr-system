package com.example.aihr.aicore.web.dto;

/**
 * 薪酬预测请求DTO
 */
public class SalaryPredictionRequestDto {
    private String employeeId;
    private double currentSalary;
    private double performanceScore;
    private double skillLevel;
    private int tenureYears;
    private String jobTitle;
    private String departmentId;
    
    // Getters and setters
    public String getEmployeeId() {
        return employeeId;
    }
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    public double getCurrentSalary() {
        return currentSalary;
    }
    public void setCurrentSalary(double currentSalary) {
        this.currentSalary = currentSalary;
    }
    public double getPerformanceScore() {
        return performanceScore;
    }
    public void setPerformanceScore(double performanceScore) {
        this.performanceScore = performanceScore;
    }
    public double getSkillLevel() {
        return skillLevel;
    }
    public void setSkillLevel(double skillLevel) {
        this.skillLevel = skillLevel;
    }
    public int getTenureYears() {
        return tenureYears;
    }
    public void setTenureYears(int tenureYears) {
        this.tenureYears = tenureYears;
    }
    public String getJobTitle() {
        return jobTitle;
    }
    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }
    public String getDepartmentId() {
        return departmentId;
    }
    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
}
