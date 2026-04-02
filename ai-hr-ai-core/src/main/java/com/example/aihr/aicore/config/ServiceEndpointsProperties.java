package com.example.aihr.aicore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aihr.services")
public class ServiceEndpointsProperties {
    private String attendanceBaseUrl;
    private String salaryBaseUrl;

    public String getAttendanceBaseUrl() { return attendanceBaseUrl; }
    public void setAttendanceBaseUrl(String attendanceBaseUrl) { this.attendanceBaseUrl = attendanceBaseUrl; }
    public String getSalaryBaseUrl() { return salaryBaseUrl; }
    public void setSalaryBaseUrl(String salaryBaseUrl) { this.salaryBaseUrl = salaryBaseUrl; }
}




