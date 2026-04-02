package com.example.aihr.aicore.service;


import com.example.aihr.aicore.web.dto.SchedulingRequestDto;
import com.example.aihr.aicore.web.dto.SchedulingResponseDto;
import com.example.aihr.aicore.web.dto.AttendanceAnomalyDetectionRequestDto;
import com.example.aihr.aicore.web.dto.AttendanceAnomalyDetectionResponseDto;
import com.example.aihr.aicore.web.dto.LeaveRequestDto;
import com.example.aihr.aicore.web.dto.LeaveApprovalResponseDto;
import com.example.aihr.aicore.web.dto.WorkHourPredictionRequestDto;
import com.example.aihr.aicore.web.dto.WorkHourPredictionResponseDto;

/**
 * 考勤AI服务，提供智能排班优化、异常打卡检测、智能请假管理和工时预测分析功能。
 */
public interface AttendanceAIService {

    /**
     * 智能排班优化
     * @param tenantId 租户ID
     * @param request 排班请求
     * @return 排班响应
     */
    SchedulingResponseDto optimizeScheduling(String tenantId, SchedulingRequestDto request);

    /**
     * 异常打卡检测
     * @param tenantId 租户ID
     * @param request 异常检测请求
     * @return 异常检测响应
     */
    AttendanceAnomalyDetectionResponseDto detectAnomalies(String tenantId, AttendanceAnomalyDetectionRequestDto request);

    /**
     * 智能请假管理
     * @param tenantId 租户ID
     * @param request 请假请求
     * @return 请假审批响应
     */
    LeaveApprovalResponseDto approveLeave(String tenantId, LeaveRequestDto request);

    /**
     * 工时预测与分析
     * @param tenantId 租户ID
     * @param request 工时预测请求
     * @return 工时预测响应
     */
    WorkHourPredictionResponseDto predictWorkHours(String tenantId, WorkHourPredictionRequestDto request);
}



