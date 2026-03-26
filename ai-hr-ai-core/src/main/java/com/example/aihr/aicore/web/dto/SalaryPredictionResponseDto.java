package com.example.aihr.aicore.web.dto;

import java.util.List;

/**
 * 薪酬预测响应DTO
 */
public class SalaryPredictionResponseDto {
    private String employeeId;
    private double currentSalary;
    private List<SalaryForecastDto> forecasts;
    private List<CareerAdviceDto> careerAdvices;
    private double averageAnnualGrowthRate;
    private double fiveYearSalaryProjection;
    
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
    public List<SalaryForecastDto> getForecasts() {
        return forecasts;
    }
    public void setForecasts(List<SalaryForecastDto> forecasts) {
        this.forecasts = forecasts;
    }
    public List<CareerAdviceDto> getCareerAdvices() {
        return careerAdvices;
    }
    public void setCareerAdvices(List<CareerAdviceDto> careerAdvices) {
        this.careerAdvices = careerAdvices;
    }
    public double getAverageAnnualGrowthRate() {
        return averageAnnualGrowthRate;
    }
    public void setAverageAnnualGrowthRate(double averageAnnualGrowthRate) {
        this.averageAnnualGrowthRate = averageAnnualGrowthRate;
    }
    public double getFiveYearSalaryProjection() {
        return fiveYearSalaryProjection;
    }
    public void setFiveYearSalaryProjection(double fiveYearSalaryProjection) {
        this.fiveYearSalaryProjection = fiveYearSalaryProjection;
    }
}
