package com.example.aihr.aicore.service;

import com.example.aihr.aicore.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AttendanceAIServiceImplTest {

    @InjectMocks
    private AttendanceAIServiceImpl attendanceAIService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testOptimizeScheduling() {
        SchedulingRequestDto request = new SchedulingRequestDto();
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(7));
        request.setEmployeeIds(List.of("emp1", "emp2", "emp3"));
        
        SchedulingRequestDto.WorkHoursDto workHours = new SchedulingRequestDto.WorkHoursDto();
        workHours.setStart(LocalTime.of(9, 0));
        workHours.setEnd(LocalTime.of(18, 0));
        request.setWorkHours(workHours);
        
        request.setMaxConsecutiveWorkingDays(5);
        request.setMinRestHoursBetweenShifts(8);
        request.setSkillRequirements(List.of("customer_service"));
        
        SchedulingResponseDto response = attendanceAIService.optimizeScheduling(request);
        assertNotNull(response);
        assertNotNull(response.getShifts());
        assertTrue(response.getShifts().size() > 0);
    }

    @Test
    public void testDetectAnomalies() {
        AttendanceAnomalyDetectionRequestDto request = new AttendanceAnomalyDetectionRequestDto();
        
        List<AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto> records = new ArrayList<>();
        AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto record = new AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto();
        record.setEmployeeId("emp1");
        record.setWorkDate(LocalDate.now());
        record.setCheckInAt(LocalDate.now().atTime(9, 30)); // 迟到
        record.setCheckOutAt(LocalDate.now().atTime(17, 30)); // 早退
        records.add(record);
        request.setAttendanceRecords(records);
        
        AttendanceAnomalyDetectionRequestDto.WorkHoursDto workHours = new AttendanceAnomalyDetectionRequestDto.WorkHoursDto();
        workHours.setStart(LocalTime.of(9, 0));
        workHours.setEnd(LocalTime.of(18, 0));
        request.setWorkHours(workHours);
        
        AttendanceAnomalyDetectionResponseDto response = attendanceAIService.detectAnomalies(request);
        assertNotNull(response);
        assertNotNull(response.getAnomalies());
    }

    @Test
    public void testApproveLeave() {
        LeaveRequestDto request = new LeaveRequestDto();
        request.setRequestId("req1");
        request.setEmployeeId("emp1");
        request.setLeaveType("PERSONAL");
        request.setDurationDays(1);
        
        LeaveApprovalResponseDto response = attendanceAIService.approveLeave(request);
        assertNotNull(response);
        assertNotNull(response.getApprovalStatus());
    }

    @Test
    public void testPredictWorkHours() {
        WorkHourPredictionRequestDto request = new WorkHourPredictionRequestDto();
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(30));
        
        WorkHourPredictionResponseDto response = attendanceAIService.predictWorkHours(request);
        assertNotNull(response);
        assertNotNull(response.getForecasts());
        assertTrue(response.getForecasts().size() > 0);
    }
}
