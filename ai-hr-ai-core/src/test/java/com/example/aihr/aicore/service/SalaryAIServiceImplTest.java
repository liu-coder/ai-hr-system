package com.example.aihr.aicore.service;

import com.example.aihr.aicore.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SalaryAIServiceImplTest {

    @InjectMocks
    private SalaryAIServiceImpl salaryAIService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSuggestSalaryAdjustment() {
        SalaryAdjustmentRequestDto request = new SalaryAdjustmentRequestDto();
        List<SalaryAdjustmentRequestDto.EmployeeSalaryDto> employees = new ArrayList<>();
        
        SalaryAdjustmentRequestDto.EmployeeSalaryDto employee = new SalaryAdjustmentRequestDto.EmployeeSalaryDto();
        employee.setEmployeeId("emp1");
        employee.setCurrentSalary(10000.0);
        employee.setMarketSalary(12000.0);
        employee.setPerformanceScore(4.5);
        employee.setTenureYears(3);
        employees.add(employee);
        
        request.setEmployees(employees);
        request.setBudgetLimit(5000.0);
        
        SalaryAdjustmentResponseDto response = salaryAIService.suggestSalaryAdjustment(request);
        assertNotNull(response);
        assertNotNull(response.getAdjustments());
        assertTrue(response.getAdjustments().size() > 0);
    }

    @Test
    public void testOptimizeTax() {
        TaxOptimizationRequestDto request = new TaxOptimizationRequestDto();
        request.setEmployeeId("emp1");
        request.setCurrentTaxableIncome(200000.0);
        request.setCurrentTax(20000.0);
        
        TaxOptimizationResponseDto response = salaryAIService.optimizeTax(request);
        assertNotNull(response);
        assertNotNull(response.getOptimizations());
        assertTrue(response.getOptimizations().size() > 0);
    }

    @Test
    public void testPredictSalary() {
        SalaryPredictionRequestDto request = new SalaryPredictionRequestDto();
        request.setEmployeeId("emp1");
        request.setCurrentSalary(10000.0);
        request.setPerformanceScore(4.0);
        request.setSkillLevel(4.0);
        
        SalaryPredictionResponseDto response = salaryAIService.predictSalary(request);
        assertNotNull(response);
        assertNotNull(response.getForecasts());
        assertTrue(response.getForecasts().size() > 0);
    }

    @Test
    public void testDetectSalaryAnomalies() {
        SalaryAnomalyDetectionRequestDto request = new SalaryAnomalyDetectionRequestDto();
        List<SalaryAnomalyDetectionRequestDto.EmployeeSalaryDataDto> salaryData = new ArrayList<>();
        
        SalaryAnomalyDetectionRequestDto.EmployeeSalaryDataDto data = new SalaryAnomalyDetectionRequestDto.EmployeeSalaryDataDto();
        data.setEmployeeId("emp1");
        data.setSalary(15000.0);
        data.setPreviousSalary(10000.0);
        data.setJobTitle("Engineer");
        data.setPerformanceScore(3.0);
        data.setTenureYears(2);
        data.setPromotion(false);
        salaryData.add(data);
        
        request.setSalaryData(salaryData);
        
        SalaryAnomalyDetectionResponseDto response = salaryAIService.detectSalaryAnomalies(request);
        assertNotNull(response);
        assertNotNull(response.getAnomalies());
    }
}
