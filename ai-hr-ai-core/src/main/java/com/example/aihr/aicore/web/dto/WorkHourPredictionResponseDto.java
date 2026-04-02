package com.example.aihr.aicore.web.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 工时预测响应DTO
 */
public class WorkHourPredictionResponseDto {
    private List<WorkHourForecastDto> forecasts;
    private double averagePredictedWorkHours;
    private double totalExpectedOvertime;
    private double totalManpowerShortage;
    private LocalDate overtimePeakDate;
    
    // Getters and setters
    public List<WorkHourForecastDto> getForecasts() {
        return forecasts;
    }
    public void setForecasts(List<WorkHourForecastDto> forecasts) {
        this.forecasts = forecasts;
    }
    public double getAveragePredictedWorkHours() {
        return averagePredictedWorkHours;
    }
    public void setAveragePredictedWorkHours(double averagePredictedWorkHours) {
        this.averagePredictedWorkHours = averagePredictedWorkHours;
    }
    public double getTotalExpectedOvertime() {
        return totalExpectedOvertime;
    }
    public void setTotalExpectedOvertime(double totalExpectedOvertime) {
        this.totalExpectedOvertime = totalExpectedOvertime;
    }
    public double getTotalManpowerShortage() {
        return totalManpowerShortage;
    }
    public void setTotalManpowerShortage(double totalManpowerShortage) {
        this.totalManpowerShortage = totalManpowerShortage;
    }
    public LocalDate getOvertimePeakDate() {
        return overtimePeakDate;
    }
    public void setOvertimePeakDate(LocalDate overtimePeakDate) {
        this.overtimePeakDate = overtimePeakDate;
    }
}



