package com.example.aihr.aicore.web.dto;

public class SalaryRequestDto {
    private String employeeId;
    private String payPeriod;
    private String comparePayPeriod;

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getPayPeriod() {
        return payPeriod;
    }

    public void setPayPeriod(String payPeriod) {
        this.payPeriod = payPeriod;
    }

    public String getComparePayPeriod() {
        return comparePayPeriod;
    }

    public void setComparePayPeriod(String comparePayPeriod) {
        this.comparePayPeriod = comparePayPeriod;
    }
}



