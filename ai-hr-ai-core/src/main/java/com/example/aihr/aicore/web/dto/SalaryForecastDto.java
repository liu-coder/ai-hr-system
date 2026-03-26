package com.example.aihr.aicore.web.dto;

/**
 * 薪酬预测DTO
 */
public class SalaryForecastDto {
    private int year;
    private double predictedSalary;
    private double growthRate;
    
    // Getters and setters
    public int getYear() {
        return year;
    }
    public void setYear(int year) {
        this.year = year;
    }
    public double getPredictedSalary() {
        return predictedSalary;
    }
    public void setPredictedSalary(double predictedSalary) {
        this.predictedSalary = predictedSalary;
    }
    public double getGrowthRate() {
        return growthRate;
    }
    public void setGrowthRate(double growthRate) {
        this.growthRate = growthRate;
    }
}
