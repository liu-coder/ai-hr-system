package com.example.aihr.attendance.service;

import com.example.aihr.common.config.RabbitMQConfig;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
@RocketMQMessageListener(
        topic = RabbitMQConfig.ATTENDANCE_ANOMALY_TOPIC,
        consumerGroup = "attendance-anomaly-consumer-group")
public class AttendanceMessageConsumer implements RocketMQListener<Map<String, Object>> {
    
    private final AttendanceService attendanceService;
    
    public AttendanceMessageConsumer(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }
    
    @Override
    public void onMessage(Map<String, Object> task) {
        try {
            String tenantId = (String) task.get("tenantId");
            String employeeId = (String) task.get("employeeId");
            String startDateStr = (String) task.get("startDate");
            String endDateStr = (String) task.get("endDate");
            String ruleSetId = (String) task.get("ruleSetId");
            
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            
            // 执行考勤异常计算
            attendanceService.computeAnomalies(tenantId, employeeId, startDate, endDate, ruleSetId);
            
            System.out.println("Successfully processed anomaly compute task for employee: " + employeeId);
        } catch (Exception e) {
            System.err.println("Error processing anomaly compute task: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
