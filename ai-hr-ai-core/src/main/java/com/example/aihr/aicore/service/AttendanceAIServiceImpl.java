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
import com.example.aihr.aicore.web.dto.LeaveApprovalDto;
import com.example.aihr.aicore.web.dto.WorkHourPredictionDto;
import com.example.aihr.aicore.web.dto.WorkHourForecastDto;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考勤AI服务实现类，提供智能排班优化、异常打卡检测、智能请假管理和工时预测分析功能。
 */
@Service
public class AttendanceAIServiceImpl implements AttendanceAIService {

    @Override
    public SchedulingResponseDto optimizeScheduling(SchedulingRequestDto request) {
        // 这里实现智能排班优化逻辑
        // 1. 收集历史排班数据、员工技能和偏好、业务需求
        // 2. 使用机器学习模型生成最优排班方案
        // 3. 评估排班方案的工作负载平衡、员工满意度等指标
        
        // 模拟员工技能和偏好数据
        Map<String, List<String>> employeeSkills = new HashMap<>();
        Map<String, Map<DayOfWeek, Integer>> employeePreferences = new HashMap<>();
        
        // 初始化员工技能和偏好
        for (String employeeId : request.getEmployeeIds()) {
            // 模拟员工技能
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
            
            // 模拟员工偏好（1-5，5表示最偏好）
            Map<DayOfWeek, Integer> preferences = new HashMap<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                preferences.put(day, (int) (Math.random() * 5) + 1);
            }
            employeePreferences.put(employeeId, preferences);
        }
        
        // 模拟业务需求波动
        Map<LocalDate, Integer> businessDemand = new HashMap<>();
        for (LocalDate date = request.getStartDate(); !date.isAfter(request.getEndDate()); date = date.plusDays(1)) {
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
        
        // 生成排班方案
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
        
        // 计算员工满意度
        double employeeSatisfactionScore = calculateEmployeeSatisfaction(shifts, employeePreferences);
        
        // 计算工作负载平衡
        double workloadBalanceScore = calculateWorkloadBalance(shifts, request.getEmployeeIds());
        
        // 计算业务需求满足度
        double businessRequirementSatisfactionScore = calculateBusinessRequirementSatisfaction(shifts, businessDemand);
        
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

    @Override
    public AttendanceAnomalyDetectionResponseDto detectAnomalies(AttendanceAnomalyDetectionRequestDto request) {
        // 这里实现异常打卡检测逻辑
        // 1. 收集多维度打卡数据
        // 2. 提取特征并使用机器学习模型检测异常
        // 3. 评估风险等级并生成预警
        
        // 模拟实现
        List<AttendanceAnomalyDto> anomalies = new ArrayList<>();
        
        // 按员工分组处理打卡记录
        Map<String, List<AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto>> employeeRecords = new HashMap<>();
        for (var record : request.getAttendanceRecords()) {
            employeeRecords.computeIfAbsent(record.getEmployeeId(), k -> new ArrayList<>()).add(record);
        }
        
        // 对每个员工的打卡记录进行分析
        for (Map.Entry<String, List<AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto>> entry : employeeRecords.entrySet()) {
            String employeeId = entry.getKey();
            List<AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto> records = entry.getValue();
            
            // 按日期排序
            records.sort((r1, r2) -> r1.getCheckInAt().compareTo(r2.getCheckInAt()));
            
            // 分析每条打卡记录
            for (int i = 0; i < records.size(); i++) {
                var record = records.get(i);
                
                // 1. 检测迟到/早退
                detectLateEarly(record, request.getWorkHours(), anomalies);
                
                // 2. 检测打卡时间异常（如深夜打卡）
                detectTimeAnomaly(record, anomalies);
                
                // 3. 检测连续打卡（短时间内多次打卡）
                if (i > 0) {
                    var previousRecord = records.get(i - 1);
                    detectConsecutiveCheckins(record, previousRecord, anomalies);
                }
                
                // 4. 检测打卡地点异常
                detectLocationAnomaly(record, request.getOfficeLocation(), anomalies);
                
                // 5. 检测替打卡（不同设备或IP地址）
                if (i > 0) {
                    var previousRecord = records.get(i - 1);
                    detectProxyCheckin(record, previousRecord, anomalies);
                }
            }
        }
        
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
            } else if (lateMinutes > 30) {
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
            } else if (earlyMinutes > 30) {
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
        
        // 检测深夜打卡（22:00 - 6:00）
        if (checkInHour >= 22 || checkInHour < 6) {
            AttendanceAnomalyDto anomaly = new AttendanceAnomalyDto();
            anomaly.setEmployeeId(record.getEmployeeId());
            anomaly.setDate(record.getWorkDate());
            anomaly.setAnomalyType("UNUSUAL_TIME_CHECKIN");
            anomaly.setDescription("深夜打卡");
            anomaly.setRiskLevel("MEDIUM");
            anomaly.setEvidence(Map.of("checkInTime", record.getCheckInAt().toString()));
            anomalies.add(anomaly);
        }
        
        if (checkOutHour >= 22 || checkOutHour < 6) {
            AttendanceAnomalyDto anomaly = new AttendanceAnomalyDto();
            anomaly.setEmployeeId(record.getEmployeeId());
            anomaly.setDate(record.getWorkDate());
            anomaly.setAnomalyType("UNUSUAL_TIME_CHECKOUT");
            anomaly.setDescription("深夜下班");
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
        
        // 如果两次打卡间隔小于30分钟，视为连续打卡
        if (minutesBetween < 30) {
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
        
        // 如果距离超过500米，视为异常地点
        if (distance > 0.5) { // 0.5公里
            AttendanceAnomalyDto anomaly = new AttendanceAnomalyDto();
            anomaly.setEmployeeId(record.getEmployeeId());
            anomaly.setDate(record.getWorkDate());
            anomaly.setAnomalyType("LOCATION_ANOMALY");
            anomaly.setDescription("打卡地点异常");
            
            // 根据距离确定风险等级
            if (distance > 5.0) { // 5公里
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

    @Override
    public LeaveApprovalResponseDto approveLeave(LeaveRequestDto request) {
        // 这里实现智能请假管理逻辑
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

    @Override
    public WorkHourPredictionResponseDto predictWorkHours(WorkHourPredictionRequestDto request) {
        // 这里实现工时预测与分析逻辑
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
