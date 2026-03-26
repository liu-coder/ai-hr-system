package com.example.aihr.aicore.web.dto;

public class WorkHourPredictionDto {
    private String employeeId;
    private String date;
    private double predictedHours;
    private double confidence;

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getPredictedHours() {
        return predictedHours;
    }

    public void setPredictedHours(double predictedHours) {
        this.predictedHours = predictedHours;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}
