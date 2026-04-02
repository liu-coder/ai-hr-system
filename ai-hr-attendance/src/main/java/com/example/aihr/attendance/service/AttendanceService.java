package com.example.aihr.attendance.service;

import com.example.aihr.attendance.domain.AttendanceAnomalyEntity;
import com.example.aihr.attendance.domain.AttendanceRecordEntity;
import com.example.aihr.attendance.domain.AttendanceRuleSetEntity;
import com.example.aihr.attendance.domain.AttendanceSnapshotEntity;
import com.example.aihr.attendance.constant.AttendanceConstants;
import com.example.aihr.common.exception.AiHrBusinessException;
import com.example.aihr.common.web.ApiError.ErrorCode;
import com.example.aihr.attendance.model.AttendanceRuleSetDto;
import com.example.aihr.attendance.model.EvidenceDto;
import com.example.aihr.attendance.model.RuleHitDto;
import com.example.aihr.attendance.repo.AttendanceAnomalyRepository;
import com.example.aihr.attendance.repo.AttendanceRecordRepository;
import com.example.aihr.attendance.repo.AttendanceRuleSetRepository;
import com.example.aihr.attendance.repo.AttendanceSnapshotRepository;
import com.example.aihr.attendance.service.AttendanceRuleEngine.AnomalyResult;
import com.example.aihr.attendance.web.dto.AnomalyResponseDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.aihr.common.validation.InputValidator;

@Service
public class AttendanceService {
    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);
    private final AttendanceRecordRepository records;
    private final AttendanceRuleSetRepository ruleSets;
    private final AttendanceSnapshotRepository snapshots;
    private final AttendanceAnomalyRepository anomalies;
    private final AttendanceRuleEngine ruleEngine;
    private final ObjectMapper om;
    private final InputValidator inputValidator;

    public AttendanceService(AttendanceRecordRepository records,
                             AttendanceRuleSetRepository ruleSets,
                             AttendanceSnapshotRepository snapshots,
                             AttendanceAnomalyRepository anomalies,
                             AttendanceRuleEngine ruleEngine,
                             ObjectMapper om,
                             InputValidator inputValidator) {
        this.records = records;
        this.ruleSets = ruleSets;
        this.snapshots = snapshots;
        this.anomalies = anomalies;
        this.ruleEngine = ruleEngine;
        this.om = om;
        this.inputValidator = inputValidator;
    }

    @Transactional
    public AttendanceRuleSetEntity createRuleSet(String tenantId, String name, String version,
                                                 LocalDate effectiveFrom, LocalDate effectiveTo,
                                                 String ruleJson, String createdBy) {
        // 验证输入
        if (!inputValidator.isValidTenantId(tenantId)) {
            throw new com.example.aihr.common.exception.AiHrBusinessException(
                com.example.aihr.common.web.ApiError.ErrorCode.PARAM_INVALID_ARGUMENT, "Invalid tenantId");
        }
        inputValidator.validateLength(name, 100, "name");
        inputValidator.validateLength(version, 50, "version");
        inputValidator.validateLength(ruleJson, 10000, "ruleJson");
        inputValidator.validateLength(createdBy, 100, "createdBy");
        
        AttendanceRuleSetEntity e = new AttendanceRuleSetEntity();
        e.setId(AttendanceConstants.RULE_SET_ID_PREFIX + UUID.randomUUID());
        e.setTenantId(tenantId);
        e.setName(name);
        e.setVersion(version);
        e.setEffectiveFrom(effectiveFrom);
        e.setEffectiveTo(effectiveTo);
        e.setStatus(AttendanceConstants.STATUS_ACTIVE);
        e.setRuleJson(ruleJson);
        e.setCreatedAt(Instant.now());
        e.setCreatedBy(createdBy);
        return ruleSets.save(e);
    }

    @Transactional
    public AttendanceRecordEntity upsertRecord(String tenantId, String employeeId, LocalDate workDate,
                                               LocalDateTime checkInAt, LocalDateTime checkOutAt,
                                               String source, Object rawPayload) {
        // 验证输入
        if (!inputValidator.isValidTenantId(tenantId)) {
            throw new com.example.aihr.common.exception.AiHrBusinessException(
                com.example.aihr.common.web.ApiError.ErrorCode.PARAM_INVALID_ARGUMENT, "Invalid tenantId");
        }
        if (!inputValidator.isValidEmployeeId(employeeId)) {
            throw new com.example.aihr.common.exception.AiHrBusinessException(
                com.example.aihr.common.web.ApiError.ErrorCode.PARAM_INVALID_ARGUMENT, "Invalid employeeId");
        }
        if (source != null) {
            inputValidator.validateLength(source, 50, "source");
        }
        
        // Simple deterministic id for (tenant, employee, date)
        String id = AttendanceConstants.RECORD_ID_PREFIX + sha256Hex(tenantId + "|" + employeeId + "|" + workDate);
        AttendanceRecordEntity e = records.findById(id).orElseGet(AttendanceRecordEntity::new);
        e.setId(id);
        e.setTenantId(tenantId);
        e.setEmployeeId(employeeId);
        e.setWorkDate(workDate);
        e.setCheckInAt(checkInAt);
        e.setCheckOutAt(checkOutAt);
        e.setSource(source == null ? AttendanceConstants.SOURCE_MANUAL : source);
        try {
            e.setRawPayload(rawPayload == null ? null : om.writeValueAsString(rawPayload));
        } catch (Exception ex) {
            log.warn("Failed to serialize raw payload for attendance record: {}", ex.getMessage());
            e.setRawPayload(null);
        }
        if (e.getCreatedAt() == null) e.setCreatedAt(Instant.now());
        return records.save(e);
    }

    @Transactional
    public ComputeResult computeAnomalies(String tenantId, String employeeId, LocalDate start, LocalDate end,
                                          String requestedRuleSetId) {
        // 验证输入
        if (!inputValidator.isValidTenantId(tenantId)) {
            throw new com.example.aihr.common.exception.AiHrBusinessException(
                com.example.aihr.common.web.ApiError.ErrorCode.PARAM_INVALID_ARGUMENT, "Invalid tenantId");
        }
        if (!inputValidator.isValidEmployeeId(employeeId)) {
            throw new com.example.aihr.common.exception.AiHrBusinessException(
                com.example.aihr.common.web.ApiError.ErrorCode.PARAM_INVALID_ARGUMENT, "Invalid employeeId");
        }
        if (start == null || end == null) {
            throw new com.example.aihr.common.exception.AiHrBusinessException(
                com.example.aihr.common.web.ApiError.ErrorCode.PARAM_INVALID_ARGUMENT, "Start and end dates are required");
        }
        if (start.isAfter(end)) {
            throw new com.example.aihr.common.exception.AiHrBusinessException(
                com.example.aihr.common.web.ApiError.ErrorCode.PARAM_INVALID_ARGUMENT, "Start date must be before end date");
        }
        if (requestedRuleSetId != null) {
            inputValidator.validateLength(requestedRuleSetId, 100, "requestedRuleSetId");
        }
        
        AttendanceRuleSetEntity ruleSet = requestedRuleSetId == null
                ? ruleSets.findActiveRuleSet(tenantId, start).orElseThrow(() -> new AiHrBusinessException(ErrorCode.ATTENDANCE_RULE_NOT_FOUND))
                : ruleSets.findById(requestedRuleSetId).orElseThrow(() -> new AiHrBusinessException(ErrorCode.ATTENDANCE_RULE_NOT_FOUND));

        List<AttendanceRecordEntity> recs = records.findByTenantIdAndEmployeeIdAndWorkDateBetween(tenantId, employeeId, start, end);
        anomalies.deleteByTenantIdAndEmployeeIdAndWorkDateBetween(tenantId, employeeId, start, end);

        String sourceHash = sha256Hex(tenantId + "|" + employeeId + "|" + start + "|" + end + "|" + recs.size() + "|" + ruleSet.getId());
        AttendanceSnapshotEntity snapshot = new AttendanceSnapshotEntity();
        snapshot.setId(AttendanceConstants.SNAPSHOT_ID_PREFIX + UUID.randomUUID());
        snapshot.setTenantId(tenantId);
        snapshot.setEmployeeId(employeeId);
        snapshot.setPeriodStart(start);
        snapshot.setPeriodEnd(end);
        snapshot.setRuleSetId(ruleSet.getId());
        snapshot.setSourceHash(sourceHash);
        snapshot.setCreatedAt(Instant.now());

        try {
            snapshot.setSnapshotJson(om.writeValueAsString(Map.of(
                    "ruleSetId", ruleSet.getId(),
                    "ruleSetVersion", ruleSet.getVersion(),
                    "start", start.toString(),
                    "end", end.toString(),
                    "records", recs
            )));
        } catch (Exception e) {
            log.warn("Failed to serialize snapshot json: {}", e.getMessage());
            snapshot.setSnapshotJson("{}");
        }
        snapshots.save(snapshot);

        AttendanceRuleSetDto ruleSetDto = ruleEngine.parseRuleSet(ruleSet.getRuleJson());
        List<AttendanceAnomalyEntity> createdAnomalies = new ArrayList<>();
        for (AttendanceRecordEntity record : recs) {
            createdAnomalies.addAll(detectAnomalies(tenantId, employeeId, record, snapshot.getId(), ruleSetDto));
        }
        anomalies.saveAll(createdAnomalies);

        return new ComputeResult(snapshot.getId(), createdAnomalies.size(), ruleSet.getId(), ruleSet.getVersion());
    }

    public List<AttendanceAnomalyEntity> listAnomalies(String tenantId, String employeeId, LocalDate start, LocalDate end) {
        return anomalies.findByTenantIdAndEmployeeIdAndWorkDateBetween(tenantId, employeeId, start, end);
    }
    
    public List<AttendanceRecordEntity> findByTenantIdAndEmployeeIdAndWorkDateBetween(String tenantId, String employeeId, LocalDate start, LocalDate end) {
        return records.findByTenantIdAndEmployeeIdAndWorkDateBetween(tenantId, employeeId, start, end);
    }

    /**
     * 将实体转为 API 响应 DTO，evidence/ruleHit 为结构化对象，便于 AI 与前端使用。
     */
    public AnomalyResponseDto toAnomalyResponse(AttendanceAnomalyEntity a) {
        Map<String, Object> evidenceMap = parseEvidence(a.getEvidenceJson());
        @SuppressWarnings("unchecked")
        Map<String, Object> facts = (Map<String, Object>) evidenceMap.getOrDefault("facts", new HashMap<String, Object>());
        String summary = evidenceMap.containsKey("summary") ? String.valueOf(evidenceMap.get("summary")) : null;
        String ruleCode = a.getRuleHit();
        String ruleId = ruleCode != null && ruleCode.startsWith("rule:") ? ruleCode.substring(5) : (ruleCode != null ? ruleCode : "");
        RuleHitDto ruleHit = new RuleHitDto(ruleId, ruleCode, summary, a.getSeverity());
        EvidenceDto evidence = new EvidenceDto(facts, summary);
        return new AnomalyResponseDto(a.getId(), a.getWorkDate(), a.getType(), a.getSeverity(), ruleHit, evidence, a.getSnapshotId());
    }

    private Map<String, Object> parseEvidence(String evidenceJson) {
        if (evidenceJson == null || evidenceJson.isBlank()) return new HashMap<>();
        try {
            return om.readValue(evidenceJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse evidence json: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private List<AttendanceAnomalyEntity> detectAnomalies(String tenantId, String employeeId, AttendanceRecordEntity record, String snapshotId,
                                                 AttendanceRuleSetDto ruleSetDto) {
        List<AttendanceAnomalyEntity> out = new ArrayList<>();
        if (ruleSetDto != null && ruleSetDto.getRules() != null && !ruleSetDto.getRules().isEmpty()) {
            for (AnomalyResult result : ruleEngine.evaluate(record, ruleSetDto)) {
                out.add(anomalyFromResult(tenantId, employeeId, result, snapshotId));
            }
            return out;
        }
        // 回退：无有效 ruleJson 时使用最小判定规则
        if (record.getCheckInAt() == null) {
            out.add(anomaly(tenantId, employeeId, record.getWorkDate(), AttendanceConstants.ANOMALY_TYPE_MISSING_CHECKIN, AttendanceConstants.SEVERITY_HIGH, AttendanceConstants.RULE_MISSING_CHECKIN, snapshotId,
                    Map.of("workDate", record.getWorkDate().toString()), "缺卡：未打卡上班"));
        } else {
            LocalTime lateThreshold = LocalTime.of(9, 5);
            if (record.getCheckInAt().toLocalTime().isAfter(lateThreshold)) {
                out.add(anomaly(tenantId, employeeId, record.getWorkDate(), AttendanceConstants.ANOMALY_TYPE_LATE, AttendanceConstants.SEVERITY_MEDIUM, AttendanceConstants.RULE_LATE_AFTER_0905, snapshotId,
                        Map.of("checkInAt", record.getCheckInAt().toString(), "threshold", lateThreshold.toString()), "迟到：上班打卡晚于 09:05"));
            }
        }
        if (record.getCheckOutAt() == null) {
            out.add(anomaly(tenantId, employeeId, record.getWorkDate(), AttendanceConstants.ANOMALY_TYPE_MISSING_CHECKOUT, AttendanceConstants.SEVERITY_HIGH, AttendanceConstants.RULE_MISSING_CHECKOUT, snapshotId,
                    Map.of("workDate", record.getWorkDate().toString()), "缺卡：未打卡下班"));
        } else {
            LocalTime earlyThreshold = LocalTime.of(18, 0);
            if (record.getCheckOutAt().toLocalTime().isBefore(earlyThreshold)) {
                out.add(anomaly(tenantId, employeeId, record.getWorkDate(), AttendanceConstants.ANOMALY_TYPE_EARLY_LEAVE, AttendanceConstants.SEVERITY_LOW, AttendanceConstants.RULE_EARLY_LEAVE_BEFORE_1800, snapshotId,
                        Map.of("checkOutAt", record.getCheckOutAt().toString(), "threshold", earlyThreshold.toString()), "早退：下班打卡早于 18:00"));
            }
        }
        return out;
    }

    private AttendanceAnomalyEntity anomalyFromResult(String tenantId, String employeeId, AnomalyResult res, String snapshotId) {
        AttendanceAnomalyEntity a = new AttendanceAnomalyEntity();
        a.setId(AttendanceConstants.ANOMALY_ID_PREFIX + UUID.randomUUID());
        a.setTenantId(tenantId);
        a.setEmployeeId(employeeId);
        a.setWorkDate(res.workDate());
        a.setType(res.type());
        a.setSeverity(res.severity());
        a.setRuleHit(res.ruleHit().toLegacyRuleHit());
        try {
            a.setEvidenceJson(om.writeValueAsString(Map.of("facts", res.evidence().facts(), "summary", res.evidence().summary())));
        } catch (Exception e) {
            log.warn("Failed to serialize evidence json: {}", e.getMessage());
            a.setEvidenceJson("{}");
        }
        a.setSnapshotId(snapshotId);
        a.setCreatedAt(Instant.now());
        return a;
    }

    private AttendanceAnomalyEntity anomaly(String tenantId, String employeeId, LocalDate workDate, String type, String severity,
                                            String ruleHit, String snapshotId, Map<String, Object> facts, String summary) {
        AttendanceAnomalyEntity a = new AttendanceAnomalyEntity();
        a.setId(AttendanceConstants.ANOMALY_ID_PREFIX + UUID.randomUUID());
        a.setTenantId(tenantId);
        a.setEmployeeId(employeeId);
        a.setWorkDate(workDate);
        a.setType(type);
        a.setSeverity(severity);
        a.setRuleHit(ruleHit);
        try {
            a.setEvidenceJson(om.writeValueAsString(Map.of("facts", facts, "summary", summary != null ? summary : "")));
        } catch (Exception e) {
            log.warn("Failed to serialize evidence json: {}", e.getMessage());
            a.setEvidenceJson("{}");
        }
        a.setSnapshotId(snapshotId);
        a.setCreatedAt(Instant.now());
        return a;
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public record ComputeResult(String snapshotId, int anomaliesCreated, String ruleSetId, String ruleSetVersion) {}
}

