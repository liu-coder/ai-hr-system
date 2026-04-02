package com.example.aihr.salary.service;

import com.example.aihr.common.config.RabbitMQConfig;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RocketMQMessageListener(
        topic = RabbitMQConfig.SALARY_PREVIEW_TOPIC,
        consumerGroup = "salary-preview-consumer-group")
public class SalaryMessageConsumer implements RocketMQListener<Map<String, Object>> {
    
    private final SalaryService salaryService;
    
    public SalaryMessageConsumer(SalaryService salaryService) {
        this.salaryService = salaryService;
    }
    
    @Override
    public void onMessage(Map<String, Object> task) {
        try {
            String tenantId = (String) task.get("tenantId");
            String payPeriod = (String) task.get("payPeriod");
            String employeeId = (String) task.get("employeeId");
            Object input = task.get("input");
            String policyId = (String) task.get("policyId");
            
            // 执行薪资预览计算
            salaryService.previewEmployee(tenantId, payPeriod, employeeId, input, policyId);
            
            System.out.println("Successfully processed salary preview task for employee: " + employeeId);
        } catch (Exception e) {
            System.err.println("Error processing salary preview task: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
