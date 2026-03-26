package com.example.aihr.attendance.service;

import com.example.aihr.attendance.domain.AttendanceRecordEntity;
import com.example.aihr.attendance.model.AttendanceRuleSetDto;
import com.example.aihr.attendance.model.EvidenceDto;
import com.example.aihr.attendance.model.RuleHitDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 考勤规则引擎：解析 ruleJson（DSL）并对单条打卡记录执行规则判定。
 * 规则不满足时返回空；满足时返回一条 AnomalyResult（含 ruleHit + evidence）。
 */
@Component
public class AttendanceRuleEngine {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ObjectMapper om = new ObjectMapper();

    /**
     * 解析 ruleJson 为 DTO；解析失败返回 null。
     */
    public AttendanceRuleSetDto parseRuleSet(String ruleJson) {
        if (ruleJson == null || ruleJson.isBlank()) return null;
        try {
            return om.readValue(ruleJson, AttendanceRuleSetDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 对单条记录执行规则集，返回命中的异常列表（每条对应一个规则命中）。
     */
    public List<AnomalyResult> evaluate(AttendanceRecordEntity record, AttendanceRuleSetDto ruleSet) {
        List<AnomalyResult> out = new ArrayList<>();
        if (ruleSet == null || ruleSet.getRules() == null) return out;

        for (AttendanceRuleSetDto.RuleDef rule : ruleSet.getRules()) {
            AnomalyResult r = evaluateOne(record, rule);
            if (r != null) out.add(r);
        }
        return out;
    }

    private AnomalyResult evaluateOne(AttendanceRecordEntity record, AttendanceRuleSetDto.RuleDef rule) {
        if (rule.getCondition() == null) return null;
        String field = rule.getCondition().getField();
        String op = rule.getCondition().getOp();
        String value = rule.getCondition().getValue();

        Map<String, Object> facts = new HashMap<>();
        facts.put("workDate", record.getWorkDate() != null ? record.getWorkDate().toString() : null);

        if ("checkInAt".equalsIgnoreCase(field)) {
            if ("null".equalsIgnoreCase(op)) {
                if (record.getCheckInAt() == null) {
                    return buildResult(record.getWorkDate(), rule, facts, "缺卡：未打卡上班");
                }
                return null;
            }
            if (record.getCheckInAt() == null) return null;
            if ("after".equalsIgnoreCase(op) && value != null) {
                LocalTime threshold = LocalTime.parse(value, TIME_FMT);
                if (record.getCheckInAt().toLocalTime().isAfter(threshold)) {
                    facts.put("checkInAt", record.getCheckInAt().toString());
                    facts.put("threshold", value);
                    return buildResult(record.getWorkDate(), rule, facts, "迟到：上班打卡晚于 " + value);
                }
            }
        }

        if ("checkOutAt".equalsIgnoreCase(field)) {
            if ("null".equalsIgnoreCase(op)) {
                if (record.getCheckOutAt() == null) {
                    return buildResult(record.getWorkDate(), rule, facts, "缺卡：未打卡下班");
                }
                return null;
            }
            if (record.getCheckOutAt() == null) return null;
            if ("before".equalsIgnoreCase(op) && value != null) {
                LocalTime threshold = LocalTime.parse(value, TIME_FMT);
                if (record.getCheckOutAt().toLocalTime().isBefore(threshold)) {
                    facts.put("checkOutAt", record.getCheckOutAt().toString());
                    facts.put("threshold", value);
                    return buildResult(record.getWorkDate(), rule, facts, "早退：下班打卡早于 " + value);
                }
            }
        }

        return null;
    }

    private AnomalyResult buildResult(java.time.LocalDate workDate, AttendanceRuleSetDto.RuleDef rule,
                                       Map<String, Object> facts, String message) {
        String ruleId = rule.getId() != null ? rule.getId() : rule.getType();
        String ruleCode = "rule:" + ruleId;
        RuleHitDto ruleHit = new RuleHitDto(ruleId, ruleCode, message, rule.getSeverity());
        EvidenceDto evidence = EvidenceDto.fromFactsWithSummary(facts, message);
        return new AnomalyResult(workDate, rule.getType(), rule.getSeverity(), ruleHit, evidence);
    }

    public record AnomalyResult(
            java.time.LocalDate workDate,
            String type,
            String severity,
            RuleHitDto ruleHit,
            EvidenceDto evidence
    ) {}
}
