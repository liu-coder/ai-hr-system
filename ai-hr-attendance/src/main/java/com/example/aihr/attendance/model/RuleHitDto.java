package com.example.aihr.attendance.model;

/**
 * 结构化 ruleHit：便于 AI 再解释与前端展示。
 */
public record RuleHitDto(
        /** 规则 ID（与规则集 ruleJson 中的 id 一致） */
        String ruleId,
        /** 规则编码，如 rule:late_after_0905，兼容旧版字符串 ruleHit */
        String ruleCode,
        /** 可读说明，供前端/AI 展示 */
        String message,
        /** 严重程度 */
        String severity
) {
    /** 兼容旧版：ruleHit 字符串即 ruleCode */
    public String toLegacyRuleHit() {
        return ruleCode != null ? ruleCode : "rule:" + ruleId;
    }
}
