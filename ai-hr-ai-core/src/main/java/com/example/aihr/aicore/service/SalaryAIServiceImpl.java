package com.example.aihr.aicore.service;

import com.example.aihr.aicore.web.dto.SalaryAdjustmentRequestDto;
import com.example.aihr.aicore.web.dto.SalaryAdjustmentResponseDto;
import com.example.aihr.aicore.web.dto.TaxOptimizationRequestDto;
import com.example.aihr.aicore.web.dto.TaxOptimizationResponseDto;
import com.example.aihr.aicore.web.dto.SalaryPredictionRequestDto;
import com.example.aihr.aicore.web.dto.SalaryPredictionResponseDto;
import com.example.aihr.aicore.web.dto.SalaryAnomalyDetectionRequestDto;
import com.example.aihr.aicore.web.dto.SalaryAnomalyDetectionResponseDto;
import com.example.aihr.aicore.web.dto.SalaryAdjustmentDto;
import com.example.aihr.aicore.web.dto.TaxOptimizationDto;
import com.example.aihr.aicore.web.dto.SalaryForecastDto;
import com.example.aihr.aicore.web.dto.CareerAdviceDto;
import com.example.aihr.aicore.web.dto.SalaryAnomalyDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 薪酬AI服务实现类
 * 
 * 设计原理：
 * 1. 采用模块化设计，将薪酬AI功能分为薪酬调整、税务优化、薪酬预测和薪酬异常检测四个核心模块
 * 2. 基于多维度数据进行分析和决策，如员工绩效、市场薪酬、内部公平性等
 * 3. 提供量化的评估指标和建议，辅助管理者进行决策
 * 4. 结合规则引擎和统计分析方法，实现智能化的薪酬管理
 * 
 * 目标：
 * 1. 提高薪酬管理的自动化和智能化水平
 * 2. 优化薪酬调整方案，确保公平性和竞争力
 * 3. 提供税务优化建议，减少员工税负
 * 4. 预测薪酬增长趋势，辅助职业规划
 * 5. 及时检测薪酬异常，确保薪酬体系的合理性
 */
@Service
public class SalaryAIServiceImpl implements SalaryAIService {

    /**
     * 智能薪酬调整建议
     * 
     * 设计原理：
     * 1. 基于多维度因素进行薪酬调整决策，包括市场薪酬、绩效表现、内部公平性和司龄
     * 2. 采用加权评分方法，综合考虑各因素的重要性
     * 3. 考虑预算限制，确保调薪方案在预算范围内
     * 4. 为每个员工生成个性化的调薪建议和理由
     * 
     * 实现思路：
     * 1. 计算部门平均薪资和绩效平均分
     * 2. 为每个员工计算市场调整、绩效调整、内部公平调整和司龄调整
     * 3. 综合各调整因素，计算总调薪金额
     * 4. 按预算限制调整调薪方案
     * 5. 生成调薪建议报告，包括总调薪金额、平均调薪比例和预算影响
     * 
     * 目标：
     * 1. 生成公平、合理的薪酬调整方案
     * 2. 确保薪酬调整与市场水平保持一致
     * 3. 激励高绩效员工，提高员工满意度
     * 4. 确保调薪方案在预算范围内
     * 5. 为管理者提供决策支持，减少人工决策的主观性
     * 
     * @param tenantId 租户ID
     * @param request 薪酬调整请求，包含员工列表和预算限制等
     * @return 薪酬调整响应，包含调薪建议和预算影响分析
     */
    @Override
    public SalaryAdjustmentResponseDto suggestSalaryAdjustment(String tenantId, SalaryAdjustmentRequestDto request) {
        // 1. 分析员工绩效、市场薪酬数据、内部公平性
        // 2. 生成个性化调薪方案
        // 3. 评估预算影响
        
        // 模拟实现
        List<SalaryAdjustmentDto> adjustments = new ArrayList<>();
        
        // 计算部门平均薪资
        double departmentAverageSalary = request.getEmployees().stream()
                .mapToDouble(e -> e.getCurrentSalary())
                .average()
                .orElse(0.0);
        
        // 计算部门绩效平均分
        double departmentAveragePerformance = request.getEmployees().stream()
                .mapToDouble(e -> e.getPerformanceScore())
                .average()
                .orElse(3.0);
        
        // 为每个员工生成调薪建议
        for (var employee : request.getEmployees()) {
            SalaryAdjustmentDto adjustment = new SalaryAdjustmentDto();
            adjustment.setEmployeeId(employee.getEmployeeId());
            adjustment.setCurrentSalary(employee.getCurrentSalary());
            
            // 计算调薪因素
            double marketAdjustment = calculateMarketAdjustment(employee);
            double performanceAdjustment = calculatePerformanceAdjustment(employee, departmentAveragePerformance);
            double internalEquityAdjustment = calculateInternalEquityAdjustment(employee, departmentAverageSalary);
            double tenureAdjustment = calculateTenureAdjustment(employee);
            
            // 综合计算调薪金额
            double totalAdjustment = marketAdjustment * 0.4 + // 市场因素权重40%
                                    performanceAdjustment * 0.3 + // 绩效因素权重30%
                                    internalEquityAdjustment * 0.2 + // 内部公平权重20%
                                    tenureAdjustment * 0.1; //  tenure权重10%
            
            // 确保调薪金额不为负
            double suggestedAdjustment = Math.max(0, totalAdjustment);
            double suggestedSalary = employee.getCurrentSalary() + suggestedAdjustment;
            
            adjustment.setSuggestedSalary(suggestedSalary);
            adjustment.setAdjustmentAmount(suggestedAdjustment);
            adjustment.setAdjustmentPercentage(suggestedAdjustment / employee.getCurrentSalary() * 100);
            
            // 生成调薪理由
            adjustment.setReason(generateAdjustmentReason(employee, marketAdjustment, performanceAdjustment, internalEquityAdjustment, tenureAdjustment));
            
            adjustments.add(adjustment);
        }
        
        // 按预算限制调整调薪方案
        adjustToBudgetLimit(adjustments, request.getBudgetLimit());
        
        SalaryAdjustmentResponseDto response = new SalaryAdjustmentResponseDto();
        response.setAdjustments(adjustments);
        response.setTotalAdjustmentAmount(adjustments.stream().mapToDouble(SalaryAdjustmentDto::getAdjustmentAmount).sum());
        response.setAverageAdjustmentPercentage(adjustments.stream().mapToDouble(SalaryAdjustmentDto::getAdjustmentPercentage).average().orElse(0.0));
        response.setBudgetImpact(response.getTotalAdjustmentAmount() * 12); // 年度预算影响
        
        return response;
    }
    
    /**
     * 计算市场调整因素
     */
    private double calculateMarketAdjustment(SalaryAdjustmentRequestDto.EmployeeSalaryDto employee) {
        double marketGap = employee.getMarketSalary() - employee.getCurrentSalary();
        // 市场调整最多不超过20%
        return Math.max(0, Math.min(employee.getCurrentSalary() * 0.2, marketGap));
    }
    
    /**
     * 计算绩效调整因素
     */
    private double calculatePerformanceAdjustment(SalaryAdjustmentRequestDto.EmployeeSalaryDto employee, double departmentAveragePerformance) {
        double performanceScore = employee.getPerformanceScore();
        double performanceFactor;
        
        if (performanceScore >= 4.5) {
            performanceFactor = 0.15; // 优秀：15%
        } else if (performanceScore >= 4.0) {
            performanceFactor = 0.10; // 良好：10%
        } else if (performanceScore >= 3.5) {
            performanceFactor = 0.05; // 达标：5%
        } else if (performanceScore >= 3.0) {
            performanceFactor = 0.02; // 基本达标：2%
        } else {
            performanceFactor = 0.0; // 不达标：0%
        }
        
        // 与部门平均绩效比较
        double relativePerformance = performanceScore / departmentAveragePerformance;
        if (relativePerformance > 1.2) {
            performanceFactor *= 1.2; // 高于部门平均20%以上，额外增加20%
        } else if (relativePerformance < 0.8) {
            performanceFactor *= 0.8; // 低于部门平均20%以上，减少20%
        }
        
        return employee.getCurrentSalary() * performanceFactor;
    }
    
    /**
     * 计算内部公平调整因素
     */
    private double calculateInternalEquityAdjustment(SalaryAdjustmentRequestDto.EmployeeSalaryDto employee, double departmentAverageSalary) {
        double salaryRatio = employee.getCurrentSalary() / departmentAverageSalary;
        double equityAdjustment;
        
        if (salaryRatio < 0.8) {
            // 低于部门平均20%以上，增加调整
            equityAdjustment = employee.getCurrentSalary() * 0.10;
        } else if (salaryRatio > 1.2) {
            // 高于部门平均20%以上，减少调整
            equityAdjustment = -employee.getCurrentSalary() * 0.05;
        } else {
            // 在合理范围内，不调整
            equityAdjustment = 0;
        }
        
        return equityAdjustment;
    }
    
    /**
     * 计算 tenure 调整因素
     */
    private double calculateTenureAdjustment(SalaryAdjustmentRequestDto.EmployeeSalaryDto employee) {
        int tenureYears = employee.getTenureYears();
        double tenureFactor;
        
        if (tenureYears >= 10) {
            tenureFactor = 0.03; // 10年以上：3%
        } else if (tenureYears >= 5) {
            tenureFactor = 0.02; // 5-9年：2%
        } else if (tenureYears >= 2) {
            tenureFactor = 0.01; // 2-4年：1%
        } else {
            tenureFactor = 0.005; // 1年以下：0.5%
        }
        
        return employee.getCurrentSalary() * tenureFactor;
    }
    
    /**
     * 生成调薪理由
     */
    private String generateAdjustmentReason(SalaryAdjustmentRequestDto.EmployeeSalaryDto employee, 
                                          double marketAdjustment, 
                                          double performanceAdjustment, 
                                          double internalEquityAdjustment, 
                                          double tenureAdjustment) {
        StringBuilder reason = new StringBuilder();
        
        if (marketAdjustment > 0) {
            reason.append("市场薪酬水平调整：").append(String.format("%.2f", marketAdjustment / employee.getCurrentSalary() * 100)).append("%；");
        }
        
        if (performanceAdjustment > 0) {
            reason.append("绩效表现调整：").append(String.format("%.2f", performanceAdjustment / employee.getCurrentSalary() * 100)).append("%；");
        }
        
        if (internalEquityAdjustment > 0) {
            reason.append("内部公平性调整：").append(String.format("%.2f", internalEquityAdjustment / employee.getCurrentSalary() * 100)).append("%；");
        }
        
        if (tenureAdjustment > 0) {
            reason.append("司龄调整：").append(String.format("%.2f", tenureAdjustment / employee.getCurrentSalary() * 100)).append("%；");
        }
        
        // 移除最后的分号
        if (reason.length() > 0) {
            reason.setLength(reason.length() - 1);
        }
        
        return reason.toString();
    }
    
    /**
     * 按预算限制调整调薪方案
     */
    private void adjustToBudgetLimit(List<SalaryAdjustmentDto> adjustments, double budgetLimit) {
        double totalAdjustment = adjustments.stream().mapToDouble(SalaryAdjustmentDto::getAdjustmentAmount).sum();
        
        // 如果总调薪金额超过预算限制，按比例缩减
        if (totalAdjustment > budgetLimit) {
            double adjustmentRatio = budgetLimit / totalAdjustment;
            
            for (SalaryAdjustmentDto adjustment : adjustments) {
                double originalAmount = adjustment.getAdjustmentAmount();
                double adjustedAmount = originalAmount * adjustmentRatio;
                adjustment.setAdjustmentAmount(adjustedAmount);
                adjustment.setSuggestedSalary(adjustment.getCurrentSalary() + adjustedAmount);
                adjustment.setAdjustmentPercentage(adjustedAmount / adjustment.getCurrentSalary() * 100);
                adjustment.setReason(adjustment.getReason() + "（因预算限制调整）");
            }
        }
    }

    /**
     * 税务优化建议
     * 
     * 设计原理：
     * 1. 基于税法规定和员工个人情况进行税务筹划
     * 2. 充分利用专项扣除、公积金等税务优惠政策
     * 3. 提供具体的优化方案和实施步骤
     * 4. 量化节税效果，评估优化方案的价值
     * 
     * 实现思路：
     * 1. 分析员工当前的税务状况
     * 2. 识别可利用的税务优惠政策
     * 3. 生成具体的税务优化建议
     * 4. 计算预期节税金额和节税比例
     * 5. 提供详细的实施步骤
     * 
     * 目标：
     * 1. 减少员工税负，提高实际收入
     * 2. 确保税务筹划的合法性和合规性
     * 3. 为员工提供个性化的税务优化建议
     * 4. 提高员工对薪酬福利的满意度
     * 5. 为管理者提供税务优化方案，增强企业的竞争力
     * 
     * @param tenantId 租户ID
     * @param request 税务优化请求，包含员工ID、当前应税收入和当前税额等
     * @return 税务优化响应，包含优化建议和节税效果分析
     */
    @Override
    public TaxOptimizationResponseDto optimizeTax(String tenantId, TaxOptimizationRequestDto request) {
        // 1. 分析员工个人情况和税法规定
        // 2. 计算最优税务筹划方案
        // 3. 分析节税效果
        
        // 模拟实现
        List<TaxOptimizationDto> optimizations = new ArrayList<>();
        
        // 专项扣除优化
        TaxOptimizationDto deductionOptimization = new TaxOptimizationDto();
        deductionOptimization.setOptimizationType("SPECIAL_DEDUCTIONS");
        deductionOptimization.setDescription("充分利用专项扣除");
        deductionOptimization.setEstimatedTaxSavings(5000);
        deductionOptimization.setImplementationSteps(List.of(
                "申请子女教育专项扣除",
                "申请住房贷款利息专项扣除",
                "申请赡养老人专项扣除"
        ));
        optimizations.add(deductionOptimization);
        
        // 公积金优化
        TaxOptimizationDto fundOptimization = new TaxOptimizationDto();
        fundOptimization.setOptimizationType("HOUSING_FUND");
        fundOptimization.setDescription("提高公积金缴纳比例");
        fundOptimization.setEstimatedTaxSavings(3000);
        fundOptimization.setImplementationSteps(List.of(
                "将公积金缴纳比例提高到上限",
                "确保单位匹配相应比例"
        ));
        optimizations.add(fundOptimization);
        
        TaxOptimizationResponseDto response = new TaxOptimizationResponseDto();
        response.setEmployeeId(request.getEmployeeId());
        response.setCurrentTaxableIncome(request.getCurrentTaxableIncome());
        response.setCurrentTax(request.getCurrentTax());
        response.setOptimizations(optimizations);
        response.setTotalEstimatedTaxSavings(optimizations.stream().mapToDouble(TaxOptimizationDto::getEstimatedTaxSavings).sum());
        response.setProjectedTax(request.getCurrentTax() - response.getTotalEstimatedTaxSavings());
        response.setTaxReductionPercentage(response.getTotalEstimatedTaxSavings() / request.getCurrentTax() * 100);
        
        return response;
    }

    /**
     * 薪酬预测与职业规划
     * 
     * 设计原理：
     * 1. 基于员工绩效、技能水平和市场趋势进行薪酬预测
     * 2. 考虑不同绩效和技能水平对薪酬增长的影响
     * 3. 提供个性化的职业发展建议，辅助员工规划职业路径
     * 4. 量化薪酬增长预测，为员工提供清晰的职业发展预期
     * 
     * 实现思路：
     * 1. 分析员工当前的绩效和技能水平
     * 2. 预测未来5年的薪酬增长趋势
     * 3. 根据绩效和技能水平调整增长率
     * 4. 生成职业发展建议，包括技能提升、绩效改进和晋升准备
     * 5. 计算平均年增长率和5年薪酬预测
     * 
     * 目标：
     * 1. 为员工提供清晰的薪酬增长预期
     * 2. 帮助员工制定合理的职业发展规划
     * 3. 激励员工提升技能和绩效
     * 4. 为管理者提供员工职业发展的参考信息
     * 5. 提高员工的工作积极性和满意度
     * 
     * @param tenantId 租户ID
     * @param request 薪酬预测请求，包含员工ID、当前薪资、绩效评分和技能水平等
     * @return 薪酬预测响应，包含薪酬预测和职业发展建议
     */
    @Override
    public SalaryPredictionResponseDto predictSalary(String tenantId, SalaryPredictionRequestDto request) {
        // 1. 分析员工绩效、技能发展、市场趋势
        // 2. 预测未来薪酬增长路径
        // 3. 提供职业发展建议
        
        // 模拟实现
        List<SalaryForecastDto> forecasts = new ArrayList<>();
        
        // 预测未来5年薪酬
        double currentSalary = request.getCurrentSalary();
        double annualGrowthRate = 0.05; // 基础年增长率
        
        for (int i = 1; i <= 5; i++) {
            SalaryForecastDto forecast = new SalaryForecastDto();
            forecast.setYear(LocalDate.now().getYear() + i);
            
            // 根据绩效和技能调整增长率
            double adjustedGrowthRate = annualGrowthRate;
            if (request.getPerformanceScore() >= 4.0) {
                adjustedGrowthRate += 0.02;
            } else if (request.getPerformanceScore() <= 2.0) {
                adjustedGrowthRate -= 0.01;
            }
            
            // 根据技能水平调整增长率
            if (request.getSkillLevel() >= 4.0) {
                adjustedGrowthRate += 0.015;
            }
            
            double predictedSalary = currentSalary * Math.pow(1 + adjustedGrowthRate, i);
            forecast.setPredictedSalary(predictedSalary);
            forecast.setGrowthRate(adjustedGrowthRate * 100);
            
            forecasts.add(forecast);
        }
        
        // 生成职业发展建议
        List<CareerAdviceDto> careerAdvices = new ArrayList<>();
        
        if (request.getSkillLevel() < 3.5) {
            CareerAdviceDto skillAdvice = new CareerAdviceDto();
            skillAdvice.setAdviceType("SKILL_ENHANCEMENT");
            skillAdvice.setDescription("提升专业技能");
            skillAdvice.setSpecificActions(List.of(
                    "参加行业相关培训",
                    "获取专业认证",
                    "参与跨部门项目"
            ));
            careerAdvices.add(skillAdvice);
        }
        
        if (request.getPerformanceScore() < 3.5) {
            CareerAdviceDto performanceAdvice = new CareerAdviceDto();
            performanceAdvice.setAdviceType("PERFORMANCE_IMPROVEMENT");
            performanceAdvice.setDescription("提高工作绩效");
            performanceAdvice.setSpecificActions(List.of(
                    "设定明确的工作目标",
                    "寻求导师指导",
                    "定期自我评估"
            ));
            careerAdvices.add(performanceAdvice);
        }
        
        CareerAdviceDto promotionAdvice = new CareerAdviceDto();
        promotionAdvice.setAdviceType("PROMOTION_PREPARATION");
        promotionAdvice.setDescription("为晋升做准备");
        promotionAdvice.setSpecificActions(List.of(
                "承担更多责任",
                "展示 leadership 能力",
                "与上级沟通职业发展目标"
        ));
        careerAdvices.add(promotionAdvice);
        
        SalaryPredictionResponseDto response = new SalaryPredictionResponseDto();
        response.setEmployeeId(request.getEmployeeId());
        response.setCurrentSalary(request.getCurrentSalary());
        response.setForecasts(forecasts);
        response.setCareerAdvices(careerAdvices);
        response.setAverageAnnualGrowthRate(forecasts.stream().mapToDouble(SalaryForecastDto::getGrowthRate).average().orElse(0.0));
        response.setFiveYearSalaryProjection(forecasts.get(forecasts.size() - 1).getPredictedSalary());
        
        return response;
    }

    /**
     * 薪酬异常检测
     * 
     * 设计原理：
     * 1. 基于多维度数据进行薪酬异常检测，包括职位、绩效、司龄等
     * 2. 采用统计分析方法，识别异常模式
     * 3. 根据异常的严重程度，划分不同的风险等级
     * 4. 提供详细的异常证据，便于人工审核
     * 
     * 实现思路：
     * 1. 按职位分组计算平均薪资
     * 2. 分析每条薪酬数据，检测同工不同酬、薪资异常增长、薪资与绩效不匹配、薪资与司龄不匹配等异常
     * 3. 根据异常的严重程度，确定风险等级
     * 4. 生成异常检测报告，包括异常数量和风险等级分布
     * 
     * 目标：
     * 1. 及时检测薪酬异常，确保薪酬体系的合理性
     * 2. 识别同工不同酬等不公平现象，促进薪酬公平性
     * 3. 发现薪资异常增长等潜在问题，防止薪酬体系失控
     * 4. 为管理者提供决策支持，及时发现和处理薪酬问题
     * 5. 提高薪酬管理的透明度和公正性
     * 
     * @param tenantId 租户ID
     * @param request 薪酬异常检测请求，包含薪酬数据列表
     * @return 薪酬异常检测响应，包含异常列表和风险等级分布
     */
    @Override
    public SalaryAnomalyDetectionResponseDto detectSalaryAnomalies(String tenantId, SalaryAnomalyDetectionRequestDto request) {
        // 1. 分析薪酬数据
        // 2. 识别异常模式
        // 3. 评估风险等级并生成预警
        
        // 模拟实现
        List<SalaryAnomalyDto> anomalies = new ArrayList<>();
        
        // 按职位分组计算平均薪资
        Map<String, Double> jobTitleAverageSalary = new HashMap<>();
        Map<String, List<SalaryAnomalyDetectionRequestDto.EmployeeSalaryDataDto>> jobTitleEmployees = new HashMap<>();
        
        for (var salaryData : request.getSalaryData()) {
            jobTitleEmployees.computeIfAbsent(salaryData.getJobTitle(), k -> new ArrayList<>()).add(salaryData);
        }
        
        for (Map.Entry<String, List<SalaryAnomalyDetectionRequestDto.EmployeeSalaryDataDto>> entry : jobTitleEmployees.entrySet()) {
            String jobTitle = entry.getKey();
            List<SalaryAnomalyDetectionRequestDto.EmployeeSalaryDataDto> employees = entry.getValue();
            double avgSalary = employees.stream().mapToDouble(e -> e.getSalary()).average().orElse(0.0);
            jobTitleAverageSalary.put(jobTitle, avgSalary);
        }
        
        // 分析每条薪酬数据
        for (var salaryData : request.getSalaryData()) {
            // 1. 检测同工不同酬（与同职位平均薪资比较）
            detectJobTitleSalaryDisparity(salaryData, jobTitleAverageSalary, anomalies);
            
            // 2. 检测薪资异常增长
            detectUnusualSalaryGrowth(salaryData, anomalies);
            
            // 3. 检测薪资与绩效不匹配
            detectPerformanceSalaryMismatch(salaryData, anomalies);
            
            // 4. 检测薪资与司龄不匹配
            detectTenureSalaryMismatch(salaryData, request.getSalaryData(), anomalies);
        }
        
        SalaryAnomalyDetectionResponseDto response = new SalaryAnomalyDetectionResponseDto();
        response.setAnomalies(anomalies);
        response.setTotalAnomalies(anomalies.size());
        response.setHighRiskAnomalies((int) anomalies.stream().filter(a -> "HIGH".equals(a.getRiskLevel())).count());
        response.setMediumRiskAnomalies((int) anomalies.stream().filter(a -> "MEDIUM".equals(a.getRiskLevel())).count());
        response.setLowRiskAnomalies((int) anomalies.stream().filter(a -> "LOW".equals(a.getRiskLevel())).count());
        
        return response;
    }
    
    /**
     * 检测同工不同酬（与同职位平均薪资比较）
     */
    private void detectJobTitleSalaryDisparity(SalaryAnomalyDetectionRequestDto.EmployeeSalaryDataDto salaryData, 
                                             Map<String, Double> jobTitleAverageSalary, 
                                             List<SalaryAnomalyDto> anomalies) {
        double averageSalary = jobTitleAverageSalary.getOrDefault(salaryData.getJobTitle(), 0.0);
        if (averageSalary > 0) {
            double salaryDifference = Math.abs(salaryData.getSalary() - averageSalary);
            double differencePercentage = salaryDifference / averageSalary * 100;
            
            if (differencePercentage > 20) {
                SalaryAnomalyDto anomaly = new SalaryAnomalyDto();
                anomaly.setEmployeeId(salaryData.getEmployeeId());
                anomaly.setAnomalyType("SALARY_DISPARITY");
                anomaly.setDescription("与同职位平均薪资差异较大");
                anomaly.setCurrentSalary(salaryData.getSalary());
                anomaly.setExpectedSalary(averageSalary);
                anomaly.setDifferenceAmount(salaryData.getSalary() - averageSalary);
                anomaly.setDifferencePercentage(differencePercentage);
                anomaly.setRiskLevel(differencePercentage > 30 ? "HIGH" : "MEDIUM");
                anomaly.setEvidence(Map.of(
                        "jobTitleAverage", averageSalary,
                        "performanceScore", salaryData.getPerformanceScore(),
                        "tenureYears", salaryData.getTenureYears()
                ));
                anomalies.add(anomaly);
            }
        }
    }
    
    /**
     * 检测薪资异常增长
     */
    private void detectUnusualSalaryGrowth(SalaryAnomalyDetectionRequestDto.EmployeeSalaryDataDto salaryData, 
                                         List<SalaryAnomalyDto> anomalies) {
        if (salaryData.getPreviousSalary() > 0) {
            double growthRate = (salaryData.getSalary() - salaryData.getPreviousSalary()) / salaryData.getPreviousSalary() * 100;
            
            // 考虑是否有晋升
            double threshold = salaryData.isPromotion() ? 50 : 30;
            
            if (growthRate > threshold) {
                SalaryAnomalyDto anomaly = new SalaryAnomalyDto();
                anomaly.setEmployeeId(salaryData.getEmployeeId());
                anomaly.setAnomalyType("UNUSUAL_GROWTH");
                anomaly.setDescription("薪资异常增长");
                anomaly.setCurrentSalary(salaryData.getSalary());
                
                // 假设正常增长率
                double normalGrowthRate = salaryData.isPromotion() ? 25 : 15;
                double expectedSalary = salaryData.getPreviousSalary() * (1 + normalGrowthRate / 100);
                
                anomaly.setExpectedSalary(expectedSalary);
                anomaly.setDifferenceAmount(salaryData.getSalary() - expectedSalary);
                anomaly.setDifferencePercentage(growthRate - normalGrowthRate);
                anomaly.setRiskLevel("HIGH");
                anomaly.setEvidence(Map.of(
                        "previousSalary", salaryData.getPreviousSalary(),
                        "growthRate", growthRate,
                        "promotion", salaryData.isPromotion(),
                        "normalGrowthRate", normalGrowthRate
                ));
                anomalies.add(anomaly);
            }
        }
    }
    
    /**
     * 检测薪资与绩效不匹配
     */
    private void detectPerformanceSalaryMismatch(SalaryAnomalyDetectionRequestDto.EmployeeSalaryDataDto salaryData, 
                                               List<SalaryAnomalyDto> anomalies) {
        double performanceScore = salaryData.getPerformanceScore();
        
        // 基于绩效的预期薪资范围
        double expectedMinSalary, expectedMaxSalary;
        
        if (performanceScore >= 4.5) {
            expectedMinSalary = salaryData.getPreviousSalary() * 1.15;
            expectedMaxSalary = salaryData.getPreviousSalary() * 1.25;
        } else if (performanceScore >= 4.0) {
            expectedMinSalary = salaryData.getPreviousSalary() * 1.08;
            expectedMaxSalary = salaryData.getPreviousSalary() * 1.15;
        } else if (performanceScore >= 3.5) {
            expectedMinSalary = salaryData.getPreviousSalary() * 1.03;
            expectedMaxSalary = salaryData.getPreviousSalary() * 1.08;
        } else if (performanceScore >= 3.0) {
            expectedMinSalary = salaryData.getPreviousSalary() * 0.98;
            expectedMaxSalary = salaryData.getPreviousSalary() * 1.03;
        } else {
            expectedMinSalary = salaryData.getPreviousSalary() * 0.95;
            expectedMaxSalary = salaryData.getPreviousSalary() * 0.98;
        }
        
        // 检查薪资是否超出预期范围
        if (salaryData.getSalary() < expectedMinSalary || salaryData.getSalary() > expectedMaxSalary) {
            SalaryAnomalyDto anomaly = new SalaryAnomalyDto();
            anomaly.setEmployeeId(salaryData.getEmployeeId());
            anomaly.setAnomalyType("PERFORMANCE_SALARY_MISMATCH");
            anomaly.setDescription("薪资与绩效不匹配");
            anomaly.setCurrentSalary(salaryData.getSalary());
            anomaly.setExpectedSalary((expectedMinSalary + expectedMaxSalary) / 2);
            anomaly.setDifferenceAmount(salaryData.getSalary() - anomaly.getExpectedSalary());
            anomaly.setDifferencePercentage(Math.abs(salaryData.getSalary() - anomaly.getExpectedSalary()) / anomaly.getExpectedSalary() * 100);
            anomaly.setRiskLevel("MEDIUM");
            anomaly.setEvidence(Map.of(
                    "performanceScore", performanceScore,
                    "expectedMinSalary", expectedMinSalary,
                    "expectedMaxSalary", expectedMaxSalary
            ));
            anomalies.add(anomaly);
        }
    }
    
    /**
     * 检测薪资与司龄不匹配
     */
    private void detectTenureSalaryMismatch(SalaryAnomalyDetectionRequestDto.EmployeeSalaryDataDto salaryData, 
                                          List<SalaryAnomalyDetectionRequestDto.EmployeeSalaryDataDto> allSalaryData, 
                                          List<SalaryAnomalyDto> anomalies) {
        int tenureYears = salaryData.getTenureYears();
        String jobTitle = salaryData.getJobTitle();
        
        // 计算同职位、相似司龄的平均薪资
        List<Double> similarTenureSalaries = new ArrayList<>();
        for (var data : allSalaryData) {
            if (data.getJobTitle().equals(jobTitle) && 
                Math.abs(data.getTenureYears() - tenureYears) <= 1) {
                similarTenureSalaries.add(data.getSalary());
            }
        }
        
        if (similarTenureSalaries.size() > 1) {
            double avgSimilarTenureSalary = similarTenureSalaries.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double salaryDifference = Math.abs(salaryData.getSalary() - avgSimilarTenureSalary);
            double differencePercentage = salaryDifference / avgSimilarTenureSalary * 100;
            
            if (differencePercentage > 20) {
                SalaryAnomalyDto anomaly = new SalaryAnomalyDto();
                anomaly.setEmployeeId(salaryData.getEmployeeId());
                anomaly.setAnomalyType("TENURE_SALARY_MISMATCH");
                anomaly.setDescription("薪资与司龄不匹配");
                anomaly.setCurrentSalary(salaryData.getSalary());
                anomaly.setExpectedSalary(avgSimilarTenureSalary);
                anomaly.setDifferenceAmount(salaryData.getSalary() - avgSimilarTenureSalary);
                anomaly.setDifferencePercentage(differencePercentage);
                anomaly.setRiskLevel(differencePercentage > 30 ? "HIGH" : "MEDIUM");
                anomaly.setEvidence(Map.of(
                        "tenureYears", tenureYears,
                        "similarTenureAverage", avgSimilarTenureSalary,
                        "similarTenureCount", similarTenureSalaries.size()
                ));
                anomalies.add(anomaly);
            }
        }
    }
}



