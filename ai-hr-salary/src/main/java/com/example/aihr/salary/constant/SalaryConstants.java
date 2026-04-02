package com.example.aihr.salary.constant;

/**
 * 薪酬模块常量类
 */
public class SalaryConstants {
    
    private SalaryConstants() {
        // 私有构造函数，防止实例化
    }
    
    // 操作类型
    public static final String OPERATION_TYPE_HUMAN = "HUMAN";
    
    // 审计操作类型
    public static final String AUDIT_OPERATION_POLICY_CREATE = "salary.policy.create";
    public static final String AUDIT_OPERATION_PREVIEW_EMPLOYEE = "salary.preview.employee";
    
    // 实体类型
    public static final String ENTITY_TYPE_POLICY = "SalaryPolicy";
    public static final String ENTITY_TYPE_CALC_RUN = "SalaryCalcRun";
    public static final String ENTITY_TYPE_TASK = "SalaryTask";
    
    // 任务ID前缀
    public static final String TASK_ID_PREFIX = "task-";
    
    // 策略ID前缀
    public static final String POLICY_ID_PREFIX = "sp-";
    
    // 计算运行ID前缀
    public static final String CALC_RUN_ID_PREFIX = "scr-";
    
    // 输入快照ID前缀
    public static final String INPUT_SNAPSHOT_ID_PREFIX = "sis-";
    
    // 结果行ID前缀
    public static final String RESULT_LINE_ID_PREFIX = "srl-";
    
    // 状态
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_SUCCESS = "SUCCESS";
    
    // 作用域类型
    public static final String SCOPE_TYPE_EMPLOYEE = "EMPLOYEE";
    
    // 货币
    public static final String CURRENCY_CNY = "CNY";
    
    // 计算版本
    public static final String CALC_VERSION_V1 = "v1";
    
    // 项目代码
    public static final String ITEM_CODE_BASE = "BASE";
    public static final String ITEM_CODE_OVERTIME = "OVERTIME";
    public static final String ITEM_CODE_BONUS = "BONUS";
    public static final String ITEM_CODE_DEDUCTIONS = "DEDUCTIONS";
    public static final String ITEM_CODE_SOCIAL = "SOCIAL";
    public static final String ITEM_CODE_HOUSING = "HOUSING";
    public static final String ITEM_CODE_TAX = "TAX";
    public static final String ITEM_CODE_NET = "NET";
    
    // 项目名称
    public static final String ITEM_NAME_BASE = "基本工资";
    public static final String ITEM_NAME_OVERTIME = "加班费";
    public static final String ITEM_NAME_BONUS = "奖金";
    public static final String ITEM_NAME_DEDUCTIONS = "其他扣款";
    public static final String ITEM_NAME_SOCIAL = "社保";
    public static final String ITEM_NAME_HOUSING = "公积金";
    public static final String ITEM_NAME_TAX = "个税";
    public static final String ITEM_NAME_NET = "实发";
}
