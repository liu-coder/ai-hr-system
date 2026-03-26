package com.example.aihr.attendance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.aihr.attendance.domain.AttendanceRecordEntity;
import com.example.aihr.attendance.domain.AttendanceRuleSetEntity;
import com.example.aihr.attendance.repo.AttendanceAnomalyRepository;
import com.example.aihr.attendance.repo.AttendanceRecordRepository;
import com.example.aihr.attendance.repo.AttendanceRuleSetRepository;
import com.example.aihr.attendance.repo.AttendanceSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {
    @Mock
    private AttendanceRecordRepository records;
    @Mock
    private AttendanceRuleSetRepository ruleSets;
    @Mock
    private AttendanceSnapshotRepository snapshots;
    @Mock
    private AttendanceAnomalyRepository anomalies;
    @Mock
    private AttendanceRuleEngine ruleEngine;

    @InjectMocks
    private AttendanceService service;

    @BeforeEach
    void setUp() {
        service = new AttendanceService(records, ruleSets, snapshots, anomalies, ruleEngine, new ObjectMapper());
    }

    @Test
    void computeAnomaliesClearsExistingRangeBeforeSavingNewOnes() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        AttendanceRuleSetEntity ruleSet = new AttendanceRuleSetEntity();
        ruleSet.setId("ars-1");
        ruleSet.setVersion("v1");
        ruleSet.setRuleJson("{}");

        AttendanceRecordEntity record = new AttendanceRecordEntity();
        record.setWorkDate(LocalDate.of(2026, 3, 3));
        record.setCheckInAt(LocalDateTime.of(2026, 3, 3, 9, 30));
        record.setCheckOutAt(LocalDateTime.of(2026, 3, 3, 18, 30));
        record.setCreatedAt(Instant.now());

        when(ruleSets.findActiveRuleSet("tenant-1", start)).thenReturn(Optional.of(ruleSet));
        when(records.findByTenantIdAndEmployeeIdAndWorkDateBetween("tenant-1", "emp-1", start, end))
                .thenReturn(List.of(record));
        when(snapshots.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceService.ComputeResult result = service.computeAnomalies("tenant-1", "emp-1", start, end, null);

        verify(anomalies).deleteByTenantIdAndEmployeeIdAndWorkDateBetween("tenant-1", "emp-1", start, end);
        verify(anomalies).saveAll(any());
        ArgumentCaptor<com.example.aihr.attendance.domain.AttendanceSnapshotEntity> snapshotCaptor = ArgumentCaptor.forClass(
                com.example.aihr.attendance.domain.AttendanceSnapshotEntity.class);
        verify(snapshots).save(snapshotCaptor.capture());
        assertEquals(start, snapshotCaptor.getValue().getPeriodStart());
        assertEquals(end, snapshotCaptor.getValue().getPeriodEnd());
        assertEquals("ars-1", result.ruleSetId());
    }
}
