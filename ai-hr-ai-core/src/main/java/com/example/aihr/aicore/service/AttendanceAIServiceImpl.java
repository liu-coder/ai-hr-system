package com.example.aihr.aicore.service;

import com.example.aihr.aicore.web.dto.SchedulingRequestDto;
import com.example.aihr.aicore.web.dto.SchedulingResponseDto;
import com.example.aihr.aicore.web.dto.AttendanceAnomalyDetectionRequestDto;
import com.example.aihr.aicore.web.dto.AttendanceAnomalyDetectionResponseDto;
import com.example.aihr.aicore.web.dto.LeaveRequestDto;
import com.example.aihr.aicore.web.dto.LeaveApprovalResponseDto;
import com.example.aihr.aicore.web.dto.WorkHourPredictionRequestDto;
import com.example.aihr.aicore.web.dto.WorkHourPredictionResponseDto;
import com.example.aihr.aicore.web.dto.SchedulingShiftDto;
import com.example.aihr.aicore.web.dto.AttendanceAnomalyDto;

import com.example.aihr.aicore.web.dto.WorkHourForecastDto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 考勤AI服务实现类
 * 
 * 设计原理：
 * 1. 采用模块化设计，将考勤AI功能分为智能排班、异常检测、请假管理和工时预测四个核心模块
 * 2. 基于规则引擎和统计分析相结合的方法，实现智能化的考勤管理
 * 3. 考虑多维度因素（如员工技能、偏好、业务需求等）进行综合决策
 * 4. 提供量化的评估指标，如员工满意度、工作负载平衡度等
 * 
 * 目标：
 * 1. 提高考勤管理的自动化和智能化水平
 * 2. 优化排班方案，提高员工满意度和工作效率
 * 3. 及时检测和预警异常考勤行为
 * 4. 预测工时需求，合理规划人力资源
 */
@Service
public class AttendanceAIServiceImpl implements AttendanceAIService {
    
    // 配置参数
    @Value("${attendance.anomaly.location.threshold:0.5}")
    private double locationAnomalyThreshold; // 位置异常阈值（公里）
    
    @Value("${attendance.anomaly.consecutive.checkin.minutes:30}")
    private int consecutiveCheckinMinutes; // 连续打卡最小间隔（分钟）
    
    @Value("${attendance.anomaly.late.threshold:30}")
    private int lateThreshold; // 迟到阈值（分钟）
    
    @Value("${attendance.anomaly.early.threshold:30}")
    private int earlyThreshold; // 早退阈值（分钟）
    
    @Value("${attendance.anomaly.time.range.start:6}")
    private int unusualTimeStart; // 异常时间开始（小时）
    
    @Value("${attendance.anomaly.time.range.end:22}")
    private int unusualTimeEnd; // 异常时间结束（小时）
    
    // Redis缓存
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 内存缓存（作为Redis的fallback）
    private final ConcurrentHashMap<String, Map<String, List<String>>> employeeSkillsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, Map<DayOfWeek, Integer>>> employeePreferencesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<LocalDate, Integer>> businessDemandCache = new ConcurrentHashMap<>();
    
    // 缓存过期时间（秒）
    private static final long CACHE_EXPIRATION_SECONDS = 3600; // 1小时
    
    // 监控指标
    private final Timer schedulingTimer;
    private final Timer anomalyDetectionTimer;
    private final Timer leaveApprovalTimer;
    private final Timer workHourPredictionTimer;
    
    private final Counter redisCacheHitCounter;
    private final Counter redisCacheMissCounter;
    private final Counter memoryCacheHitCounter;
    private final Counter memoryCacheMissCounter;
    
    private final Counter anomalyCounter;
    private final Counter highRiskAnomalyCounter;
    private final Counter mediumRiskAnomalyCounter;
    private final Counter lowRiskAnomalyCounter;
    
    private final AtomicLong redisCacheSize;
    private final AtomicLong memoryCacheSize;
    
    // 构造函数注入RedisTemplate和MeterRegistry
    public AttendanceAIServiceImpl(RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        if (redisTemplate == null) {
            throw new IllegalArgumentException("RedisTemplate不能为null");
        }
        this.redisTemplate = redisTemplate;
        
        // 初始化监控指标
        if (meterRegistry != null) {
            this.schedulingTimer = Timer.builder("attendance.ai.scheduling.duration")
                    .description("Time taken to optimize scheduling")
                    .register(meterRegistry);
            
            this.anomalyDetectionTimer = Timer.builder("attendance.ai.anomaly.detection.duration")
                    .description("Time taken to detect anomalies")
                    .register(meterRegistry);
            
            this.leaveApprovalTimer = Timer.builder("attendance.ai.leave.approval.duration")
                    .description("Time taken to approve leave")
                    .register(meterRegistry);
            
            this.workHourPredictionTimer = Timer.builder("attendance.ai.work.hour.prediction.duration")
                    .description("Time taken to predict work hours")
                    .register(meterRegistry);
            
            this.redisCacheHitCounter = Counter.builder("attendance.ai.cache.redis.hit")
                    .description("Redis cache hits")
                    .register(meterRegistry);
            
            this.redisCacheMissCounter = Counter.builder("attendance.ai.cache.redis.miss")
                    .description("Redis cache misses")
                    .register(meterRegistry);
            
            this.memoryCacheHitCounter = Counter.builder("attendance.ai.cache.memory.hit")
                    .description("Memory cache hits")
                    .register(meterRegistry);
            
            this.memoryCacheMissCounter = Counter.builder("attendance.ai.cache.memory.miss")
                    .description("Memory cache misses")
                    .register(meterRegistry);
            
            this.anomalyCounter = Counter.builder("attendance.ai.anomaly.total")
                    .description("Total anomalies detected")
                    .register(meterRegistry);
            
            this.highRiskAnomalyCounter = Counter.builder("attendance.ai.anomaly.high.risk")
                    .description("High risk anomalies detected")
                    .register(meterRegistry);
            
            this.mediumRiskAnomalyCounter = Counter.builder("attendance.ai.anomaly.medium.risk")
                    .description("Medium risk anomalies detected")
                    .register(meterRegistry);
            
            this.lowRiskAnomalyCounter = Counter.builder("attendance.ai.anomaly.low.risk")
                    .description("Low risk anomalies detected")
                    .register(meterRegistry);
            
            this.redisCacheSize = new AtomicLong(0);
            Gauge.builder("attendance.ai.cache.redis.size", this.redisCacheSize, AtomicLong::get)
                    .description("Redis cache size")
                    .register(meterRegistry);
            
            this.memoryCacheSize = new AtomicLong(0);
            Gauge.builder("attendance.ai.cache.memory.size", this.memoryCacheSize, AtomicLong::get)
                    .description("Memory cache size")
                    .register(meterRegistry);
        } else {
            // 测试环境下的默认值
            this.schedulingTimer = null;
            this.anomalyDetectionTimer = null;
            this.leaveApprovalTimer = null;
            this.workHourPredictionTimer = null;
            this.redisCacheHitCounter = null;
            this.redisCacheMissCounter = null;
            this.memoryCacheHitCounter = null;
            this.memoryCacheMissCounter = null;
            this.anomalyCounter = null;
            this.highRiskAnomalyCounter = null;
            this.mediumRiskAnomalyCounter = null;
            this.lowRiskAnomalyCounter = null;
            this.redisCacheSize = new AtomicLong(0);
            this.memoryCacheSize = new AtomicLong(0);
        }
    }

    /**
     * 智能排班优化
     * 
     * 设计原理：
     * 1. 基于多维度因素进行排班决策，包括员工技能、偏好、业务需求等
     * 2. 采用贪心算法，优先选择技能匹配度高且偏好度高的员工
     * 3. 考虑员工连续工作天数和休息时间等约束条件
     * 4. 提供量化的评估指标，评估排班方案的质量
     * 
     * 实现思路：
     * 1. 模拟员工技能和偏好数据
     * 2. 模拟业务需求波动
     * 3. 为每个日期生成排班，考虑连续工作天数和休息时间约束
     * 4. 根据技能匹配度和偏好排序员工
     * 5. 计算员工满意度、工作负载平衡度和业务需求满足度
     * 
     * 目标：
     * 1. 生成最优的排班方案，满足业务需求
     * 2. 提高员工满意度，减少员工抱怨
     * 3. 平衡工作负载，避免个别员工过度劳累
     * 4. 提高排班效率，减少人工排班的时间和精力
     * 
     * @param tenantId 租户ID
     * @param request 排班请求，包含员工ID列表、开始和结束日期、工作时间、技能要求等
     * @return 排班响应，包含排班方案和评估指标
     */
    @Override
    public SchedulingResponseDto optimizeScheduling(String tenantId, SchedulingRequestDto request) {
        if (schedulingTimer != null) {
            return schedulingTimer.record(() -> {
                return doOptimizeScheduling(tenantId, request);
            });
        } else {
            return doOptimizeScheduling(tenantId, request);
        }
    }
    
    private SchedulingResponseDto doOptimizeScheduling(String tenantId, SchedulingRequestDto request) {
        // 1. 收集历史排班数据、员工技能和偏好、业务需求
        Map<String, List<String>> employeeSkills = initializeEmployeeSkills(tenantId, request.getEmployeeIds());
        Map<String, Map<DayOfWeek, Integer>> employeePreferences = initializeEmployeePreferences(tenantId, request.getEmployeeIds());
        Map<LocalDate, Integer> businessDemand = initializeBusinessDemand(request.getStartDate(), request.getEndDate());
        
        // 2. 生成排班方案
        List<SchedulingShiftDto> shifts = generateSchedulingShifts(request, employeeSkills, employeePreferences, businessDemand);
        
        // 3. 评估排班方案
        double employeeSatisfactionScore = calculateEmployeeSatisfaction(shifts, employeePreferences);
        double workloadBalanceScore = calculateWorkloadBalance(shifts, request.getEmployeeIds());
        double businessRequirementSatisfactionScore = calculateBusinessRequirementSatisfaction(shifts, businessDemand);
        
        // 4. 构建响应
        return buildSchedulingResponse(shifts, employeeSatisfactionScore, workloadBalanceScore, businessRequirementSatisfactionScore);
    }
    
    /**
     * 初始化员工技能数据
     */
    private Map<String, List<String>> initializeEmployeeSkills(String tenantId, List<String> employeeIds) {
        String cacheKey = "employee_skills:" + tenantId;
        
        // 尝试从Redis获取
        try {
            Map<String, List<String>> cachedSkills = (Map<String, List<String>>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedSkills != null) {
                if (redisCacheHitCounter != null) {
                    redisCacheHitCounter.increment();
                }
                return cachedSkills;
            }
        } catch (Exception e) {
            // Redis失败，使用内存缓存
        }
        if (redisCacheMissCounter != null) {
            redisCacheMissCounter.increment();
        }
        
        // 尝试从内存缓存获取
        Map<String, List<String>> cachedSkills = employeeSkillsCache.get(tenantId);
        if (cachedSkills != null) {
            if (memoryCacheHitCounter != null) {
                memoryCacheHitCounter.increment();
            }
            return cachedSkills;
        }
        if (memoryCacheMissCounter != null) {
            memoryCacheMissCounter.increment();
        }
        
        // 模拟员工技能数据
        Map<String, List<String>> employeeSkills = new HashMap<>();
        for (String employeeId : employeeIds) {
            List<String> skills = new ArrayList<>();
            if (Math.random() > 0.5) {
                skills.add("customer_service");
            }
            if (Math.random() > 0.3) {
                skills.add("technical_support");
            }
            if (Math.random() > 0.7) {
                skills.add("management");
            }
            employeeSkills.put(employeeId, skills);
        }
        
        // 缓存到Redis
        try {
            redisTemplate.opsForValue().set(cacheKey, employeeSkills, CACHE_EXPIRATION_SECONDS, TimeUnit.SECONDS);
            if (redisCacheSize != null) {
                redisCacheSize.incrementAndGet();
            }
        } catch (Exception e) {
            // Redis失败，只缓存到内存
        }
        
        // 缓存到内存
        employeeSkillsCache.put(tenantId, employeeSkills);
        if (memoryCacheSize != null) {
            memoryCacheSize.incrementAndGet();
        }
        return employeeSkills;
    }
    
    /**
     * 初始化员工偏好数据
     */
    private Map<String, Map<DayOfWeek, Integer>> initializeEmployeePreferences(String tenantId, List<String> employeeIds) {
        String cacheKey = "employee_preferences:" + tenantId;
        
        // 尝试从Redis获取
        try {
            Map<String, Map<DayOfWeek, Integer>> cachedPreferences = (Map<String, Map<DayOfWeek, Integer>>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedPreferences != null) {
                return cachedPreferences;
            }
        } catch (Exception e) {
            // Redis失败，使用内存缓存
        }
        
        // 尝试从内存缓存获取
        Map<String, Map<DayOfWeek, Integer>> cachedPreferences = employeePreferencesCache.get(tenantId);
        if (cachedPreferences != null) {
            return cachedPreferences;
        }
        
        // 模拟员工偏好数据
        Map<String, Map<DayOfWeek, Integer>> employeePreferences = new HashMap<>();
        for (String employeeId : employeeIds) {
            Map<DayOfWeek, Integer> preferences = new HashMap<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                preferences.put(day, (int) (Math.random() * 5) + 1);
            }
            employeePreferences.put(employeeId, preferences);
        }
        
        // 缓存到Redis
        try {
            redisTemplate.opsForValue().set(cacheKey, employeePreferences, CACHE_EXPIRATION_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Redis失败，只缓存到内存
        }
        
        // 缓存到内存
        employeePreferencesCache.put(tenantId, employeePreferences);
        return employeePreferences;
    }
    
    /**
     * 初始化业务需求数据
     */
    private Map<LocalDate, Integer> initializeBusinessDemand(LocalDate startDate, LocalDate endDate) {
        String cacheKey = "business_demand:" + startDate + "_" + endDate;
        
        // 尝试从Redis获取
        try {
            Map<LocalDate, Integer> cachedDemand = (Map<LocalDate, Integer>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedDemand != null) {
                return cachedDemand;
            }
        } catch (Exception e) {
            // Redis失败，使用内存缓存
        }
        
        // 尝试从内存缓存获取
        String memoryCacheKey = startDate + "_" + endDate;
        Map<LocalDate, Integer> cachedDemand = businessDemandCache.get(memoryCacheKey);
        if (cachedDemand != null) {
            return cachedDemand;
        }
        
        // 模拟业务需求波动
        Map<LocalDate, Integer> businessDemand = new HashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // 基础需求
            int baseDemand = 2;
            // 周末需求较低
            if (date.getDayOfWeek().getValue() >= 6) {
                baseDemand = 1;
            }
            // 月初和月末需求较高
            if (date.getDayOfMonth() <= 5 || date.getDayOfMonth() >= 25) {
                baseDemand = 3;
            }
            // 随机波动
            baseDemand += (int) (Math.random() * 2);
            businessDemand.put(date, baseDemand);
        }
        
        // 缓存到Redis
        try {
            redisTemplate.opsForValue().set(cacheKey, businessDemand, CACHE_EXPIRATION_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Redis失败，只缓存到内存
        }
        
        // 缓存到内存
        businessDemandCache.put(memoryCacheKey, businessDemand);
        return businessDemand;
    }
    
    /**
     * 生成排班方案
     */
    private List<SchedulingShiftDto> generateSchedulingShifts(SchedulingRequestDto request, 
                                                           Map<String, List<String>> employeeSkills, 
                                                           Map<String, Map<DayOfWeek, Integer>> employeePreferences, 
                                                           Map<LocalDate, Integer> businessDemand) {
        List<SchedulingShiftDto> shifts = new ArrayList<>();
        Map<String, Integer> consecutiveWorkingDays = new HashMap<>();
        Map<String, LocalDateTime> lastShiftEndTime = new HashMap<>();
        
        // 初始化员工连续工作天数和最后班次结束时间
        for (String employeeId : request.getEmployeeIds()) {
            consecutiveWorkingDays.put(employeeId, 0);
            lastShiftEndTime.put(employeeId, LocalDateTime.now().minusDays(1));
        }
        
        // 为每个日期生成排班
        LocalDate currentDate = request.getStartDate();
        while (!currentDate.isAfter(request.getEndDate())) {
            final LocalDate date = currentDate;
            int demand = businessDemand.get(date);
            List<String> availableEmployees = new ArrayList<>(request.getEmployeeIds());
            
            // 过滤掉连续工作天数超过限制的员工
            availableEmployees.removeIf(employeeId -> consecutiveWorkingDays.get(employeeId) >= request.getMaxConsecutiveWorkingDays());
            
            // 过滤掉休息时间不足的员工
            availableEmployees.removeIf(employeeId -> {
                LocalDateTime lastEnd = lastShiftEndTime.get(employeeId);
                LocalDateTime nextStart = LocalDateTime.of(date, request.getWorkHours().getStart());
                Duration duration = Duration.between(lastEnd, nextStart);
                return duration.toHours() < request.getMinRestHoursBetweenShifts();
            });
            
            // 根据技能匹配度和偏好排序员工
            availableEmployees.sort((e1, e2) -> {
                // 计算技能匹配度
                int skillMatch1 = calculateSkillMatch(employeeSkills.get(e1), request.getSkillRequirements());
                int skillMatch2 = calculateSkillMatch(employeeSkills.get(e2), request.getSkillRequirements());
                
                // 计算偏好得分
                int preference1 = employeePreferences.get(e1).get(date.getDayOfWeek());
                int preference2 = employeePreferences.get(e2).get(date.getDayOfWeek());
                
                // 综合得分（技能匹配度权重70%，偏好权重30%）
                double score1 = skillMatch1 * 0.7 + preference1 * 0.3;
                double score2 = skillMatch2 * 0.7 + preference2 * 0.3;
                
                return Double.compare(score2, score1); // 降序排序
            });
            
            // 选择前demand个员工进行排班
            int scheduled = 0;
            for (String employeeId : availableEmployees) {
                if (scheduled >= demand) {
                    break;
                }
                
                SchedulingShiftDto shift = new SchedulingShiftDto();
                shift.setEmployeeId(employeeId);
                shift.setDate(date);
                shift.setStartTime(LocalDateTime.of(date, request.getWorkHours().getStart()));
                shift.setEndTime(LocalDateTime.of(date, request.getWorkHours().getEnd()));
                shift.setSkillRequirements(request.getSkillRequirements());
                shifts.add(shift);
                
                // 更新员工连续工作天数和最后班次结束时间
                consecutiveWorkingDays.put(employeeId, consecutiveWorkingDays.get(employeeId) + 1);
                lastShiftEndTime.put(employeeId, shift.getEndTime());
                
                scheduled++;
            }
            
            // 对于未排班的员工，重置连续工作天数
            for (String employeeId : request.getEmployeeIds()) {
                if (!shifts.stream().anyMatch(s -> s.getEmployeeId().equals(employeeId) && s.getDate().equals(date))) {
                    consecutiveWorkingDays.put(employeeId, 0);
                }
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        return shifts;
    }
    
    /**
     * 构建排班响应
     */
    private SchedulingResponseDto buildSchedulingResponse(List<SchedulingShiftDto> shifts, 
                                                       double employeeSatisfactionScore, 
                                                       double workloadBalanceScore, 
                                                       double businessRequirementSatisfactionScore) {
        SchedulingResponseDto response = new SchedulingResponseDto();
        response.setShifts(shifts);
        response.setEmployeeSatisfactionScore(employeeSatisfactionScore);
        response.setWorkloadBalanceScore(workloadBalanceScore);
        response.setBusinessRequirementSatisfactionScore(businessRequirementSatisfactionScore);
        return response;
    }
    
    /**
     * 计算技能匹配度
     */
    private int calculateSkillMatch(List<String> employeeSkills, List<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return 5;
        }
        
        int matchCount = 0;
        for (String skill : requiredSkills) {
            if (employeeSkills.contains(skill)) {
                matchCount++;
            }
        }
        
        return (int) (matchCount / (double) requiredSkills.size() * 5);
    }
    
    /**
     * 计算员工满意度
     */
    private double calculateEmployeeSatisfaction(List<SchedulingShiftDto> shifts, Map<String, Map<DayOfWeek, Integer>> employeePreferences) {
        if (shifts.isEmpty()) {
            return 0;
        }
        
        double totalSatisfaction = 0;
        for (SchedulingShiftDto shift : shifts) {
            String employeeId = shift.getEmployeeId();
            DayOfWeek day = shift.getDate().getDayOfWeek();
            int preference = employeePreferences.get(employeeId).get(day);
            totalSatisfaction += preference;
        }
        
        return (totalSatisfaction / (shifts.size() * 5)) * 100;
    }
    
    /**
     * 计算工作负载平衡
     */
    private double calculateWorkloadBalance(List<SchedulingShiftDto> shifts, List<String> employeeIds) {
        if (shifts.isEmpty()) {
            return 0;
        }
        
        // 计算每个员工的排班次数
        Map<String, Integer> workloadMap = new HashMap<>();
        for (String employeeId : employeeIds) {
            workloadMap.put(employeeId, 0);
        }
        
        for (SchedulingShiftDto shift : shifts) {
            workloadMap.put(shift.getEmployeeId(), workloadMap.get(shift.getEmployeeId()) + 1);
        }
        
        // 计算标准差
        List<Integer> workloads = new ArrayList<>(workloadMap.values());
        double mean = workloads.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = workloads.stream().mapToDouble(w -> Math.pow(w - mean, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);
        
        // 计算平衡得分（标准差越小，得分越高）
        double maxPossibleStdDev = mean; // 最大可能的标准差
        return (1 - (stdDev / maxPossibleStdDev)) * 100;
    }
    
    /**
     * 计算业务需求满足度
     */
    private double calculateBusinessRequirementSatisfaction(List<SchedulingShiftDto> shifts, Map<LocalDate, Integer> businessDemand) {
        if (businessDemand.isEmpty()) {
            return 0;
        }
        
        double totalSatisfaction = 0;
        for (Map.Entry<LocalDate, Integer> entry : businessDemand.entrySet()) {
            LocalDate date = entry.getKey();
            int demand = entry.getValue();
            int scheduled = (int) shifts.stream().filter(s -> s.getDate().equals(date)).count();
            
            // 计算当天的满足度（不超过100%）
            double daySatisfaction = Math.min(100, (scheduled / (double) demand) * 100);
            totalSatisfaction += daySatisfaction;
        }
        
        return totalSatisfaction / businessDemand.size();
    }

    /**
     * 异常打卡检测
     * 
     * 设计原理：
     * 1. 基于多维度数据进行异常检测，包括时间、地点、设备等
     * 2. 采用规则引擎和统计分析相结合的方法
     * 3. 根据异常的严重程度，划分不同的风险等级
     * 4. 提供详细的异常证据，便于人工审核
     * 
     * 实现思路：
     * 1. 按员工分组处理打卡记录
     * 2. 分析每条打卡记录，检测迟到/早退、深夜打卡、连续打卡、地点异常、替打卡等
     * 3. 根据异常的严重程度，确定风险等级
     * 4. 生成异常检测报告，包括异常数量和风险等级分布
     * 
     * 目标：
     * 1. 及时检测和预警异常考勤行为
     * 2. 减少考勤作弊行为，提高考勤数据的真实性
     * 3. 为管理者提供决策支持，及时发现和处理考勤问题
     * 4. 提高考勤管理的自动化水平，减少人工审核的工作量
     * 
     * @param tenantId 租户ID
     * @param request 异常检测请求，包含打卡记录、工作时间、办公室位置等
     * @return 异常检测响应，包含异常列表和风险等级分布
     */
    @Override
    public AttendanceAnomalyDetectionResponseDto detectAnomalies(String tenantId, AttendanceAnomalyDetectionRequestDto request) {
        if (anomalyDetectionTimer != null) {
            return anomalyDetectionTimer.record(() -> {
                return doDetectAnomalies(tenantId, request);
            });
        } else {
            return doDetectAnomalies(tenantId, request);
        }
    }
    
    private AttendanceAnomalyDetectionResponseDto doDetectAnomalies(String tenantId, AttendanceAnomalyDetectionRequestDto request) {
        // 1. 收集多维度打卡数据
        // 2. 提取特征并使用机器学习模型检测异常
        // 3. 评估风险等级并生成预警
        
        // 模拟实现
        List<AttendanceAnomalyDto> anomalies = new ArrayList<>();
        
        // 按员工分组处理打卡记录
        Map<String, List<AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto>> employeeRecords = groupRecordsByEmployee(request.getAttendanceRecords());
        
        // 对每个员工的打卡记录进行分析
        for (Map.Entry<String, List<AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto>> entry : employeeRecords.entrySet()) {
            List<AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto> records = entry.getValue();
            
            // 按日期排序
            records.sort((r1, r2) -> r1.getCheckInAt().compareTo(r2.getCheckInAt()));
            
            // 分析每条打卡记录
            analyzeAttendanceRecords(records, request.getWorkHours(), request.getOfficeLocation(), anomalies);
        }
        
        // 记录异常检测指标
        recordAnomalyMetrics(anomalies);
        
        // 构建响应
        return buildAnomalyDetectionResponse(anomalies);
    }
    
    /**
     * 记录异常检测指标
     */
    private void recordAnomalyMetrics(List<AttendanceAnomalyDto> anomalies) {
        if (anomalyCounter != null) {
            anomalyCounter.increment(anomalies.size());
        }
        
        for (AttendanceAnomalyDto anomaly : anomalies) {
            switch (anomaly.getRiskLevel()) {
                case "HIGH":
                    if (highRiskAnomalyCounter != null) {
                        highRiskAnomalyCounter.increment();
                    }
                    break;
                case "MEDIUM":
                    if (mediumRiskAnomalyCounter != null) {
                        mediumRiskAnomalyCounter.increment();
                    }
                    break;
                case "LOW":
                    if (lowRiskAnomalyCounter != null) {
                        lowRiskAnomalyCounter.increment();
                    }
                    break;
            }
        }
    }
    
    /**
     * 按员工分组打卡记录
     */
    private Map<String, List<AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto>> groupRecordsByEmployee(
            List<AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto> records) {
        Map<String, List<AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto>> employeeRecords = new HashMap<>();
        for (var record : records) {
            employeeRecords.computeIfAbsent(record.getEmployeeId(), k -> new ArrayList<>()).add(record);
        }
        return employeeRecords;
    }
    
    /**
     * 分析打卡记录，检测异常
     */
    private void analyzeAttendanceRecords(List<AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto> records, 
                                        AttendanceAnomalyDetectionRequestDto.WorkHoursDto workHours, 
                                        AttendanceAnomalyDetectionRequestDto.LocationDto officeLocation, 
                                        List<AttendanceAnomalyDto> anomalies) {
        for (int i = 0; i < records.size(); i++) {
            var record = records.get(i);
            
            // 1. 检测迟到/早退
            detectLateEarly(record, workHours, anomalies);
            
            // 2. 检测打卡时间异常（如深夜打卡）
            detectTimeAnomaly(record, anomalies);
            
            // 3. 检测连续打卡（短时间内多次打卡）
            if (i > 0) {
                var previousRecord = records.get(i - 1);
                detectConsecutiveCheckins(record, previousRecord, anomalies);
            }
            
            // 4. 检测打卡地点异常
            detectLocationAnomaly(record, officeLocation, anomalies);
            
            // 5. 检测替打卡（不同设备或IP地址）
            if (i > 0) {
                var previousRecord = records.get(i - 1);
                detectProxyCheckin(record, previousRecord, anomalies);
            }
        }
    }
    
    /**
     * 构建异常检测响应
     */
    private AttendanceAnomalyDetectionResponseDto buildAnomalyDetectionResponse(List<AttendanceAnomalyDto> anomalies) {
        AttendanceAnomalyDetectionResponseDto response = new AttendanceAnomalyDetectionResponseDto();
        response.setAnomalies(anomalies);
        response.setTotalAnomalies(anomalies.size());
        response.setHighRiskAnomalies((int) anomalies.stream().filter(a -> "HIGH".equals(a.getRiskLevel())).count());
        response.setMediumRiskAnomalies((int) anomalies.stream().filter(a -> "MEDIUM".equals(a.getRiskLevel())).count());
        response.setLowRiskAnomalies((int) anomalies.stream().filter(a -> "LOW".equals(a.getRiskLevel())).count());
        return response;
    }
    
    /**
     * 检测迟到/早退
     */
    private void detectLateEarly(AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto record, 
                                AttendanceAnomalyDetectionRequestDto.WorkHoursDto workHours, 
                                List<AttendanceAnomalyDto> anomalies) {
        // 检测迟到
        if (record.getCheckInAt().toLocalTime().isAfter(workHours.getStart())) {
            AttendanceAnomalyDto anomaly = new AttendanceAnomalyDto();
            anomaly.setEmployeeId(record.getEmployeeId());
            anomaly.setDate(record.getWorkDate());
            anomaly.setAnomalyType("LATE_CHECKIN");
            anomaly.setDescription("员工迟到");
            
            // 计算迟到时间（分钟）
            int lateMinutes = (int) Duration.between(workHours.getStart(), record.getCheckInAt().toLocalTime()).toMinutes();
            
            // 根据迟到时间确定风险等级
            if (lateMinutes > 60) {
                anomaly.setRiskLevel("HIGH");
            } else if (lateMinutes > lateThreshold) {
                anomaly.setRiskLevel("MEDIUM");
            } else {
                anomaly.setRiskLevel("LOW");
            }
            
            anomaly.setEvidence(Map.of(
                    "checkInTime", record.getCheckInAt().toString(),
                    "scheduledStart", workHours.getStart().toString(),
                    "lateMinutes", String.valueOf(lateMinutes)
            ));
            anomalies.add(anomaly);
        }
        
        // 检测早退
        if (record.getCheckOutAt().toLocalTime().isBefore(workHours.getEnd())) {
            AttendanceAnomalyDto anomaly = new AttendanceAnomalyDto();
            anomaly.setEmployeeId(record.getEmployeeId());
            anomaly.setDate(record.getWorkDate());
            anomaly.setAnomalyType("EARLY_CHECKOUT");
            anomaly.setDescription("员工早退");
            
            // 计算早退时间（分钟）
            int earlyMinutes = (int) Duration.between(record.getCheckOutAt().toLocalTime(), workHours.getEnd()).toMinutes();
            
            // 根据早退时间确定风险等级
            if (earlyMinutes > 60) {
                anomaly.setRiskLevel("HIGH");
            } else if (earlyMinutes > earlyThreshold) {
                anomaly.setRiskLevel("MEDIUM");
            } else {
                anomaly.setRiskLevel("LOW");
            }
            
            anomaly.setEvidence(Map.of(
                    "checkOutTime", record.getCheckOutAt().toString(),
                    "scheduledEnd", workHours.getEnd().toString(),
                    "earlyMinutes", String.valueOf(earlyMinutes)
            ));
            anomalies.add(anomaly);
        }
    }
    
    /**
     * 检测打卡时间异常（如深夜打卡）
     */
    private void detectTimeAnomaly(AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto record, 
                                 List<AttendanceAnomalyDto> anomalies) {
        int checkInHour = record.getCheckInAt().getHour();
        int checkOutHour = record.getCheckOutAt().getHour();
        
        // 检测异常时间打卡
        if (checkInHour >= unusualTimeEnd || checkInHour < unusualTimeStart) {
            AttendanceAnomalyDto anomaly = new AttendanceAnomalyDto();
            anomaly.setEmployeeId(record.getEmployeeId());
            anomaly.setDate(record.getWorkDate());
            anomaly.setAnomalyType("UNUSUAL_TIME_CHECKIN");
            anomaly.setDescription("异常时间打卡");
            anomaly.setRiskLevel("MEDIUM");
            anomaly.setEvidence(Map.of("checkInTime", record.getCheckInAt().toString()));
            anomalies.add(anomaly);
        }
        
        if (checkOutHour >= unusualTimeEnd || checkOutHour < unusualTimeStart) {
            AttendanceAnomalyDto anomaly = new AttendanceAnomalyDto();
            anomaly.setEmployeeId(record.getEmployeeId());
            anomaly.setDate(record.getWorkDate());
            anomaly.setAnomalyType("UNUSUAL_TIME_CHECKOUT");
            anomaly.setDescription("异常时间下班");
            anomaly.setRiskLevel("MEDIUM");
            anomaly.setEvidence(Map.of("checkOutTime", record.getCheckOutAt().toString()));
            anomalies.add(anomaly);
        }
    }
    
    /**
     * 检测连续打卡（短时间内多次打卡）
     */
    private void detectConsecutiveCheckins(AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto current, 
                                         AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto previous, 
                                         List<AttendanceAnomalyDto> anomalies) {
        // 计算两次打卡之间的时间差（分钟）
        Duration duration = Duration.between(previous.getCheckInAt(), current.getCheckInAt());
        long minutesBetween = duration.toMinutes();
        
        // 如果两次打卡间隔小于设定值，视为连续打卡
        if (minutesBetween < consecutiveCheckinMinutes) {
            AttendanceAnomalyDto anomaly = new AttendanceAnomalyDto();
            anomaly.setEmployeeId(current.getEmployeeId());
            anomaly.setDate(current.getWorkDate());
            anomaly.setAnomalyType("CONSECUTIVE_CHECKINS");
            anomaly.setDescription("短时间内多次打卡");
            anomaly.setRiskLevel("MEDIUM");
            anomaly.setEvidence(Map.of(
                    "currentCheckIn", current.getCheckInAt().toString(),
                    "previousCheckIn", previous.getCheckInAt().toString(),
                    "minutesBetween", String.valueOf(minutesBetween)
            ));
            anomalies.add(anomaly);
        }
    }
    
    /**
     * 检测打卡地点异常
     */
    private void detectLocationAnomaly(AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto record, 
                                     AttendanceAnomalyDetectionRequestDto.LocationDto officeLocation, 
                                     List<AttendanceAnomalyDto> anomalies) {
        if (record.getLocation() == null || officeLocation == null) {
            return;
        }
        
        // 计算打卡地点与办公室的距离（简单的欧几里得距离）
        double distance = calculateDistance(
                record.getLocation().getLatitude(), record.getLocation().getLongitude(),
                officeLocation.getLatitude(), officeLocation.getLongitude()
        );
        
        // 如果距离超过设定阈值，视为异常地点
        if (distance > locationAnomalyThreshold) {
            AttendanceAnomalyDto anomaly = new AttendanceAnomalyDto();
            anomaly.setEmployeeId(record.getEmployeeId());
            anomaly.setDate(record.getWorkDate());
            anomaly.setAnomalyType("LOCATION_ANOMALY");
            anomaly.setDescription("打卡地点异常");
            
            // 根据距离确定风险等级
            if (distance > locationAnomalyThreshold * 10) { // 10倍阈值
                anomaly.setRiskLevel("HIGH");
            } else {
                anomaly.setRiskLevel("MEDIUM");
            }
            
            anomaly.setEvidence(Map.of(
                    "checkInLocation", record.getLocation().toString(),
                    "officeLocation", officeLocation.toString(),
                    "distance", String.format("%.2f km", distance)
            ));
            anomalies.add(anomaly);
        }
    }
    
    /**
     * 检测替打卡（不同设备或IP地址）
     */
    private void detectProxyCheckin(AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto current, 
                                   AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto previous, 
                                   List<AttendanceAnomalyDto> anomalies) {
        // 检查设备ID是否不同
        if (current.getDeviceId() != null && previous.getDeviceId() != null && 
            !current.getDeviceId().equals(previous.getDeviceId())) {
            AttendanceAnomalyDto anomaly = new AttendanceAnomalyDto();
            anomaly.setEmployeeId(current.getEmployeeId());
            anomaly.setDate(current.getWorkDate());
            anomaly.setAnomalyType("DEVICE_CHANGE");
            anomaly.setDescription("设备变更");
            anomaly.setRiskLevel("MEDIUM");
            anomaly.setEvidence(Map.of(
                    "currentDevice", current.getDeviceId(),
                    "previousDevice", previous.getDeviceId()
            ));
            anomalies.add(anomaly);
        }
        
        // 检查IP地址是否不同
        if (current.getIpAddress() != null && previous.getIpAddress() != null && 
            !current.getIpAddress().equals(previous.getIpAddress())) {
            AttendanceAnomalyDto anomaly = new AttendanceAnomalyDto();
            anomaly.setEmployeeId(current.getEmployeeId());
            anomaly.setDate(current.getWorkDate());
            anomaly.setAnomalyType("IP_CHANGE");
            anomaly.setDescription("IP地址变更");
            anomaly.setRiskLevel("MEDIUM");
            anomaly.setEvidence(Map.of(
                    "currentIp", current.getIpAddress(),
                    "previousIp", previous.getIpAddress()
            ));
            anomalies.add(anomaly);
        }
    }
    
    /**
     * 计算两点之间的距离（公里）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 智能请假管理
     * 
     * 设计原理：
     * 1. 基于请假类型和时长进行风险评估
     * 2. 实现自动化审批，提高效率
     * 3. 对高风险请假进行人工审批，保证审批质量
     * 4. 分析请假对团队工作负载和项目进度的影响
     * 
     * 实现思路：
     * 1. 根据请假类型和时长进行简单的风险评估
     * 2. 自动批准低风险请假（如个人假1-2天，病假1-3天）
     * 3. 高风险请假（如请假时长较长）转人工审批
     * 4. 分析请假对团队工作负载和项目进度的影响
     * 
     * 目标：
     * 1. 提高请假审批的自动化水平，减少人工审批的工作量
     * 2. 快速处理低风险请假，提高员工满意度
     * 3. 确保高风险请假得到充分审核，避免对业务造成影响
     * 4. 为管理者提供请假影响分析，辅助决策
     * 
     * @param tenantId 租户ID
     * @param request 请假请求，包含请求ID、员工ID、请假类型、请假时长等
     * @return 请假审批响应，包含审批状态、审批消息、风险等级和请假影响分析
     */
    @Override
    public LeaveApprovalResponseDto approveLeave(String tenantId, LeaveRequestDto request) {
        if (leaveApprovalTimer != null) {
            return leaveApprovalTimer.record(() -> {
                return doApproveLeave(tenantId, request);
            });
        } else {
            return doApproveLeave(tenantId, request);
        }
    }
    
    private LeaveApprovalResponseDto doApproveLeave(String tenantId, LeaveRequestDto request) {
        // 1. 分析员工历史请假记录、工作饱和度和团队排班情况
        // 2. 自动审批低风险请假
        // 3. 高风险请假转人工审批
        
        // 模拟实现
        LeaveApprovalResponseDto response = new LeaveApprovalResponseDto();
        response.setRequestId(request.getRequestId());
        response.setEmployeeId(request.getEmployeeId());
        
        // 简单的审批逻辑，实际应使用更复杂的算法
        if (request.getLeaveType().equals("PERSONAL") && request.getDurationDays() <= 2) {
            response.setApprovalStatus("APPROVED_AUTOMATICALLY");
            response.setApprovalMessage("请假已自动批准");
            response.setIsHighRisk(false);
        } else if (request.getLeaveType().equals("SICK") && request.getDurationDays() <= 3) {
            response.setApprovalStatus("APPROVED_AUTOMATICALLY");
            response.setApprovalMessage("请假已自动批准");
            response.setIsHighRisk(false);
        } else {
            response.setApprovalStatus("PENDING_MANUAL_APPROVAL");
            response.setApprovalMessage("请假需要人工审批");
            response.setIsHighRisk(true);
        }
        
        // 分析请假影响
        response.setTeamWorkloadImpact(30.0);
        response.setProjectScheduleImpact(20.0);
        
        return response;
    }

    /**
     * 工时预测与分析
     * 
     * 设计原理：
     * 1. 基于历史工时数据和业务周期规律进行预测
     * 2. 考虑不同时间段的工时需求差异，如周末和月末
     * 3. 识别加班高峰和人力不足时段，提前预警
     * 4. 提供量化的预测指标，辅助人力资源规划
     * 
     * 实现思路：
     * 1. 分析历史工时数据，识别业务周期规律
     * 2. 预测未来工时需求，考虑周末和月末的特殊情况
     * 3. 计算预期加班时间和人力不足情况
     * 4. 识别加班高峰日期，提前预警
     * 
     * 目标：
     * 1. 准确预测未来工时需求，合理规划人力资源
     * 2. 识别加班高峰和人力不足时段，提前采取措施
     * 3. 优化人力配置，提高工作效率
     * 4. 为管理者提供决策支持，辅助人力资源规划
     * 
     * @param tenantId 租户ID
     * @param request 工时预测请求，包含开始和结束日期等
     * @return 工时预测响应，包含工时预测、预期加班时间、人力不足情况和加班高峰日期
     */
    @Override
    public WorkHourPredictionResponseDto predictWorkHours(String tenantId, WorkHourPredictionRequestDto request) {
        if (workHourPredictionTimer != null) {
            return workHourPredictionTimer.record(() -> {
                return doPredictWorkHours(tenantId, request);
            });
        } else {
            return doPredictWorkHours(tenantId, request);
        }
    }
    
    private WorkHourPredictionResponseDto doPredictWorkHours(String tenantId, WorkHourPredictionRequestDto request) {
        // 1. 分析历史工时数据
        // 2. 预测未来工时需求
        // 3. 识别加班高峰和人力不足时段
        
        // 模拟实现
        List<WorkHourForecastDto> forecasts = new ArrayList<>();
        
        // 简单的预测逻辑，实际应使用更复杂的算法
        for (LocalDate date = request.getStartDate(); !date.isAfter(request.getEndDate()); date = date.plusDays(1)) {
            WorkHourForecastDto forecast = new WorkHourForecastDto();
            forecast.setDate(date);
            
            // 模拟预测工时需求
            double baseHours = 8.0;
            // 周末工时需求较低
            if (date.getDayOfWeek().getValue() >= 6) {
                baseHours = 4.0;
            }
            // 月初和月末工时需求较高
            if (date.getDayOfMonth() <= 5 || date.getDayOfMonth() >= 25) {
                baseHours *= 1.5;
            }
            
            forecast.setPredictedWorkHours(baseHours);
            forecast.setExpectedOvertime(baseHours > 8.0 ? baseHours - 8.0 : 0.0);
            forecast.setManpowerShortage(baseHours > 10.0 ? baseHours - 10.0 : 0.0);
            
            forecasts.add(forecast);
        }
        
        WorkHourPredictionResponseDto response = new WorkHourPredictionResponseDto();
        response.setForecasts(forecasts);
        response.setAveragePredictedWorkHours(forecasts.stream().mapToDouble(WorkHourForecastDto::getPredictedWorkHours).average().orElse(0.0));
        response.setTotalExpectedOvertime(forecasts.stream().mapToDouble(WorkHourForecastDto::getExpectedOvertime).sum());
        response.setTotalManpowerShortage(forecasts.stream().mapToDouble(WorkHourForecastDto::getManpowerShortage).sum());
        
        // 识别加班高峰
        LocalDate overtimePeakDate = forecasts.stream()
                .max((a, b) -> Double.compare(a.getExpectedOvertime(), b.getExpectedOvertime()))
                .map(WorkHourForecastDto::getDate)
                .orElse(null);
        response.setOvertimePeakDate(overtimePeakDate);
        
        return response;
    }
}



