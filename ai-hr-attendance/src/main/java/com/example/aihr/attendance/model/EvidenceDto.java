package com.example.aihr.attendance.model;

import java.util.Map;

/**
 * 结构化 evidence：事实键值 + 可选摘要，便于 AI 再解释与前端展示。
 */
public record EvidenceDto(
        /** 事实键值对，如 workDate、checkInAt、threshold 等 */
        Map<String, Object> facts,
        /** 可选：一句话摘要，供前端/AI 直接展示 */
        String summary
) {
    public static EvidenceDto fromFacts(Map<String, Object> facts) {
        return new EvidenceDto(facts, null);
    }

    public static EvidenceDto fromFactsWithSummary(Map<String, Object> facts, String summary) {
        return new EvidenceDto(facts, summary);
    }
}
