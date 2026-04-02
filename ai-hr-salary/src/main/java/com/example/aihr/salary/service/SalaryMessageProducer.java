package com.example.aihr.salary.service;

import com.example.aihr.common.config.RabbitMQConfig;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SalaryMessageProducer {
    
    private final RocketMQTemplate rocketMQTemplate;
    
    public SalaryMessageProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }
    
    public void sendSalaryPreviewTask(String tenantId, String payPeriod, String employeeId, Object input, String policyId) {
        Map<String, Object> task = new HashMap<>();
        task.put("tenantId", tenantId);
        task.put("payPeriod", payPeriod);
        task.put("employeeId", employeeId);
        if (input != null) {
            task.put("input", input);
        }
        if (policyId != null) {
            task.put("policyId", policyId);
        }
        
        rocketMQTemplate.convertAndSend(RabbitMQConfig.SALARY_PREVIEW_TOPIC, task);
    }
}
