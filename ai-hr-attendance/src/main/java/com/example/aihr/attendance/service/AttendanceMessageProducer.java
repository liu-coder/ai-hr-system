package com.example.aihr.attendance.service;

import com.example.aihr.common.config.RabbitMQConfig;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AttendanceMessageProducer {
    
    private final RocketMQTemplate rocketMQTemplate;
    
    public AttendanceMessageProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }
    
    public void sendAnomalyComputeTask(String tenantId, String employeeId, String startDate, String endDate, String ruleSetId) {
        Map<String, Object> task = Map.of(
            "tenantId", tenantId,
            "employeeId", employeeId,
            "startDate", startDate,
            "endDate", endDate,
            "ruleSetId", ruleSetId
        );
        
        rocketMQTemplate.convertAndSend(RabbitMQConfig.ATTENDANCE_ANOMALY_TOPIC, task);
    }
}
