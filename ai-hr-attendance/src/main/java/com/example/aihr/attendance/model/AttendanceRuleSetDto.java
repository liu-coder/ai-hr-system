package com.example.aihr.attendance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * DSL 模型：考勤规则集 ruleJson 的可执行结构。
 * 后续可扩展为完整规则引擎（如 Groovy/SpEL/自定义 DSL）。
 *
 * 示例 ruleJson：
 * {
 *   "rules": [
 *     { "id": "missing_checkin", "type": "MISSING_CHECKIN", "condition": { "field": "checkInAt", "op": "null" }, "severity": "HIGH" },
 *     { "id": "late_after", "type": "LATE", "condition": { "field": "checkInAt", "op": "after", "value": "09:05" }, "severity": "MEDIUM" },
 *     { "id": "early_leave", "type": "EARLY_LEAVE", "condition": { "field": "checkOutAt", "op": "before", "value": "18:00" }, "severity": "LOW" }
 *   ]
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttendanceRuleSetDto {

    private List<RuleDef> rules;

    public List<RuleDef> getRules() {
        return rules;
    }

    public void setRules(List<RuleDef> rules) {
        this.rules = rules;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RuleDef {
        /** 规则唯一标识，用于 ruleHit */
        private String id;
        /** 异常类型：MISSING_CHECKIN, MISSING_CHECKOUT, LATE, EARLY_LEAVE 等 */
        private String type;
        /** 严重程度：HIGH, MEDIUM, LOW */
        private String severity;
        /** 条件：field + op + 可选 value（时间用 HH:mm） */
        private Condition condition;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public Condition getCondition() { return condition; }
        public void setCondition(Condition condition) { this.condition = condition; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Condition {
        /** 字段：checkInAt, checkOutAt */
        private String field;
        /** 操作：null, after, before */
        private String op;
        /** 比较值，如 "09:05"（仅 after/before 需要） */
        private String value;

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getOp() { return op; }
        public void setOp(String op) { this.op = op; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}
