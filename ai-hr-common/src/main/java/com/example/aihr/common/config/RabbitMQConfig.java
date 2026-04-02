package com.example.aihr.common.config;

/**
 * 通用消息主题配置（RocketMQ）
 */
public class RabbitMQConfig {
    
    // 考勤相关主题
    public static final String ATTENDANCE_ANOMALY_TOPIC = "attendance_anomaly_compute";
    
    // 薪资相关主题
    public static final String SALARY_PREVIEW_TOPIC = "salary_preview_compute";
}
