package com.example.aihr.aicore.web.dto;

import java.util.List;

/**
 * 薪酬异常检测请求DTO
 */
public class SalaryAnomalyDetectionRequestDto {
    private String departmentId;
    private double departmentAverageSalary;
    private List<EmployeeSalaryDataDto> salaryData;
    private String payPeriod;
    
    // Getters and setters
    public String getDepartmentId() {
        return departmentId;
    }
    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
    public double getDepartmentAverageSalary() {
        return departmentAverageSalary;
    }
    public void setDepartmentAverageSalary(double departmentAverageSalary) {
        this.departmentAverageSalary = departmentAverageSalary;
    }
    public List<EmployeeSalaryDataDto> getSalaryData() {
        return salaryData;
    }
    public void setSalaryData(List<EmployeeSalaryDataDto> salaryData) {
        this.salaryData = salaryData;
    }
    public String getPayPeriod() {
        return payPeriod;
    }
    public void setPayPeriod(String payPeriod) {
        this.payPeriod = payPeriod;
    }
    
    /**
     * 员工薪酬数据DTO
     */
    public static class EmployeeSalaryDataDto {
        private String employeeId;
        private double salary;
        private double previousSalary;
        private double performanceScore;
        private int tenureYears;
        private String jobTitle;
        private boolean promotion;
        
        // Getters and setters
        public String getEmployeeId() {
            return employeeId;
        }
        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }
        public double getSalary() {
            return salary;
        }
        public void setSalary(double salary) {
            this.salary = salary;
        }
        public double getPreviousSalary() {
            return previousSalary;
        }
        public void setPreviousSalary(double previousSalary) {
            this.previousSalary = previousSalary;
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
        public boolean isPromotion() {
            return promotion;
        }
        public void setPromotion(boolean promotion) {
            this.promotion = promotion;
        }
    }
}
