package com.example.aihr.attendance.web.dto;

import com.example.aihr.attendance.model.EvidenceDto;
import com.example.aihr.attendance.model.RuleHitDto;
import java.time.LocalDate;

/**
 * 考勤异常查询 API 的响应 DTO：ruleHit / evidence 为结构化对象，便于 AI 再解释与前端展示。
 */
public record AnomalyResponseDto(
        String id,
        LocalDate workDate,
        String type,
        String severity,
        /** 命中的规则（结构化） */
        RuleHitDto ruleHit,
        /** 证据（事实 + 摘要，结构化） */
        EvidenceDto evidence,
        String snapshotId
) {}
