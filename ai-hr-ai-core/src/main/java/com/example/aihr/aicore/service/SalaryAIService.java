package com.example.aihr.aicore.service;

import com.example.aihr.aicore.web.dto.SalaryAdjustmentRequestDto;
import com.example.aihr.aicore.web.dto.SalaryAdjustmentResponseDto;
import com.example.aihr.aicore.web.dto.TaxOptimizationRequestDto;
import com.example.aihr.aicore.web.dto.TaxOptimizationResponseDto;
import com.example.aihr.aicore.web.dto.SalaryPredictionRequestDto;
import com.example.aihr.aicore.web.dto.SalaryPredictionResponseDto;
import com.example.aihr.aicore.web.dto.SalaryAnomalyDetectionRequestDto;
import com.example.aihr.aicore.web.dto.SalaryAnomalyDetectionResponseDto;

/**
 * 薪酬AI服务，提供智能薪酬调整建议、税务优化建议、薪酬预测与职业规划和薪酬异常检测功能。
 */
public interface SalaryAIService {

    /**
     * 智能薪酬调整建议
     * @param tenantId 租户ID
     * @param request 薪酬调整请求
     * @return 薪酬调整响应
     */
    SalaryAdjustmentResponseDto suggestSalaryAdjustment(String tenantId, SalaryAdjustmentRequestDto request);

    /**
     * 税务优化建议
     * @param tenantId 租户ID
     * @param request 税务优化请求
     * @return 税务优化响应
     */
    TaxOptimizationResponseDto optimizeTax(String tenantId, TaxOptimizationRequestDto request);

    /**
     * 薪酬预测与职业规划
     * @param tenantId 租户ID
     * @param request 薪酬预测请求
     * @return 薪酬预测响应
     */
    SalaryPredictionResponseDto predictSalary(String tenantId, SalaryPredictionRequestDto request);

    /**
     * 薪酬异常检测
     * @param tenantId 租户ID
     * @param request 薪酬异常检测请求
     * @return 薪酬异常检测响应
     */
    SalaryAnomalyDetectionResponseDto detectSalaryAnomalies(String tenantId, SalaryAnomalyDetectionRequestDto request);
}



