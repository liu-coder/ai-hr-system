package com.example.aihr.attendance.constant;

/**
 * 考勤模块常量类
 */
public class AttendanceConstants {
    
    private AttendanceConstants() {
        // 私有构造函数，防止实例化
    }
    
    // 操作类型
    public static final String OPERATION_TYPE_HUMAN = "HUMAN";
    
    // 审计操作类型
    public static final String AUDIT_OPERATION_RULESET_CREATE = "attendance.ruleset.create";
    public static final String AUDIT_OPERATION_RECORD_UPSERT = "attendance.record.upsert";
    public static final String AUDIT_OPERATION_ANOMALY_COMPUTE = "attendance.anomaly.compute";
    
    // 实体类型
    public static final String ENTITY_TYPE_RULE_SET = "AttendanceRuleSet";
    public static final String ENTITY_TYPE_RECORD = "AttendanceRecord";
    public static final String ENTITY_TYPE_SNAPSHOT = "AttendanceSnapshot";
    public static final String ENTITY_TYPE_TASK = "AttendanceTask";
    
    // 任务ID前缀
    public static final String TASK_ID_PREFIX = "task-";
    
    // 规则集ID前缀
    public static final String RULE_SET_ID_PREFIX = "ars-";
    
    // 记录ID前缀
    public static final String RECORD_ID_PREFIX = "ar-";
    
    // 快照ID前缀
    public static final String SNAPSHOT_ID_PREFIX = "as-";
    
    // 异常ID前缀
    public static final String ANOMALY_ID_PREFIX = "aa-";
    
    // 状态
    public static final String STATUS_ACTIVE = "ACTIVE";
    
    // 来源
    public static final String SOURCE_MANUAL = "MANUAL";
    
    // 异常类型
    public static final String ANOMALY_TYPE_MISSING_CHECKIN = "MISSING_CHECKIN";
    public static final String ANOMALY_TYPE_LATE = "LATE";
    public static final String ANOMALY_TYPE_MISSING_CHECKOUT = "MISSING_CHECKOUT";
    public static final String ANOMALY_TYPE_EARLY_LEAVE = "EARLY_LEAVE";
    
    // 严重程度
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_MEDIUM = "MEDIUM";
    public static final String SEVERITY_LOW = "LOW";
    
    // 规则ID
    public static final String RULE_MISSING_CHECKIN = "rule:missing_checkin";
    public static final String RULE_LATE_AFTER_0905 = "rule:late_after_0905";
    public static final String RULE_MISSING_CHECKOUT = "rule:missing_checkout";
    public static final String RULE_EARLY_LEAVE_BEFORE_1800 = "rule:early_leave_before_1800";
}
