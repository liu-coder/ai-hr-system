package com.example.aihr.aicore.web.dto;

/**
 * 请假审批响应DTO
 */
public class LeaveApprovalResponseDto {
    private String requestId;
    private String employeeId;
    private String approvalStatus;
    private String approvalMessage;
    private boolean isHighRisk;
    private double teamWorkloadImpact;
    private double projectScheduleImpact;
    
    // Getters and setters
    public String getRequestId() {
        return requestId;
    }
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    public String getEmployeeId() {
        return employeeId;
    }
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    public String getApprovalStatus() {
        return approvalStatus;
    }
    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }
    public String getApprovalMessage() {
        return approvalMessage;
    }
    public void setApprovalMessage(String approvalMessage) {
        this.approvalMessage = approvalMessage;
    }
    public boolean isHighRisk() {
        return isHighRisk;
    }
    public void setIsHighRisk(boolean isHighRisk) {
        this.isHighRisk = isHighRisk;
    }
    public double getTeamWorkloadImpact() {
        return teamWorkloadImpact;
    }
    public void setTeamWorkloadImpact(double teamWorkloadImpact) {
        this.teamWorkloadImpact = teamWorkloadImpact;
    }
    public double getProjectScheduleImpact() {
        return projectScheduleImpact;
    }
    public void setProjectScheduleImpact(double projectScheduleImpact) {
        this.projectScheduleImpact = projectScheduleImpact;
    }
}
