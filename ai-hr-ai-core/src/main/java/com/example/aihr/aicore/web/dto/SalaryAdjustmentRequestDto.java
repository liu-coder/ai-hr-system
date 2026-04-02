package com.example.aihr.aicore.web.dto;

import java.util.List;

/**
 * 薪酬调整请求DTO
 */
public class SalaryAdjustmentRequestDto {
    private String departmentId;
    private List<EmployeeSalaryDto> employees;
    private double budgetLimit;
    private String payPeriod;
    
    // Getters and setters
    public String getDepartmentId() {
        return departmentId;
    }
    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
    public List<EmployeeSalaryDto> getEmployees() {
        return employees;
    }
    public void setEmployees(List<EmployeeSalaryDto> employees) {
        this.employees = employees;
    }
    public double getBudgetLimit() {
        return budgetLimit;
    }
    public void setBudgetLimit(double budgetLimit) {
        this.budgetLimit = budgetLimit;
    }
    public String getPayPeriod() {
        return payPeriod;
    }
    public void setPayPeriod(String payPeriod) {
        this.payPeriod = payPeriod;
    }
    
    /**
     * 员工薪酬DTO
     */
    public static class EmployeeSalaryDto {
        private String employeeId;
        private double currentSalary;
        private double marketSalary;
        private double performanceScore;
        private int tenureYears;
        private String jobTitle;
        
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
        public double getMarketSalary() {
            return marketSalary;
        }
        public void setMarketSalary(double marketSalary) {
            this.marketSalary = marketSalary;
        }
        public double getPerformanceScore() {
            return performanceScore;
        }
        public void setPerformanceScore(double performanceScore) {
            this.performanceScore = performanceScore;
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
    }
}



