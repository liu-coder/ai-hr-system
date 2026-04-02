package com.example.aihr.aicore.service;

import com.example.aihr.aicore.web.dto.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AttendanceAIServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private AttendanceAIServiceImpl attendanceAIService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // 手动创建实例，传入null作为MeterRegistry
        attendanceAIService = new AttendanceAIServiceImpl(redisTemplate, null);
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
        
        SchedulingResponseDto response = attendanceAIService.optimizeScheduling("tenant1", request);
        assertNotNull(response);
        assertNotNull(response.getShifts());
        assertTrue(response.getShifts().size() > 0);
        assertTrue(response.getEmployeeSatisfactionScore() >= 0 && response.getEmployeeSatisfactionScore() <= 100);
        assertTrue(response.getWorkloadBalanceScore() >= 0 && response.getWorkloadBalanceScore() <= 100);
        assertTrue(response.getBusinessRequirementSatisfactionScore() >= 0 && response.getBusinessRequirementSatisfactionScore() <= 100);
    }

    @Test
    public void testOptimizeSchedulingWithCache() {
        // 第一次调用，应该初始化缓存
        SchedulingRequestDto request = new SchedulingRequestDto();
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(7));
        request.setEmployeeIds(List.of("emp1", "emp2"));
        
        SchedulingRequestDto.WorkHoursDto workHours = new SchedulingRequestDto.WorkHoursDto();
        workHours.setStart(LocalTime.of(9, 0));
        workHours.setEnd(LocalTime.of(18, 0));
        request.setWorkHours(workHours);
        
        request.setMaxConsecutiveWorkingDays(5);
        request.setMinRestHoursBetweenShifts(8);
        request.setSkillRequirements(List.of("customer_service"));
        
        // 第一次调用
        long startTime1 = System.currentTimeMillis();
        SchedulingResponseDto response1 = attendanceAIService.optimizeScheduling("tenant2", request);
        long time1 = System.currentTimeMillis() - startTime1;
        
        // 第二次调用，应该使用缓存
        long startTime2 = System.currentTimeMillis();
        SchedulingResponseDto response2 = attendanceAIService.optimizeScheduling("tenant2", request);
        long time2 = System.currentTimeMillis() - startTime2;
        
        // 验证两次调用都成功
        assertNotNull(response1);
        assertNotNull(response2);
        assertTrue(response1.getShifts().size() > 0);
        assertTrue(response2.getShifts().size() > 0);
        
        // 第二次调用应该更快（使用缓存）
        assertTrue(time2 <= time1);
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
        
        AttendanceAnomalyDetectionResponseDto response = attendanceAIService.detectAnomalies("tenant1", request);
        assertNotNull(response);
        assertNotNull(response.getAnomalies());
        assertTrue(response.getTotalAnomalies() >= 0);
    }

    @Test
    public void testDetectAnomaliesWithLocation() {
        AttendanceAnomalyDetectionRequestDto request = new AttendanceAnomalyDetectionRequestDto();
        
        List<AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto> records = new ArrayList<>();
        AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto record = new AttendanceAnomalyDetectionRequestDto.AttendanceRecordDto();
        record.setEmployeeId("emp1");
        record.setWorkDate(LocalDate.now());
        record.setCheckInAt(LocalDate.now().atTime(9, 0));
        record.setCheckOutAt(LocalDate.now().atTime(18, 0));
        
        // 设置打卡地点（与办公室位置不同）
        AttendanceAnomalyDetectionRequestDto.LocationDto location = new AttendanceAnomalyDetectionRequestDto.LocationDto();
        location.setLatitude(31.2304);
        location.setLongitude(121.4737); // 上海
        record.setLocation(location);
        records.add(record);
        request.setAttendanceRecords(records);
        
        AttendanceAnomalyDetectionRequestDto.WorkHoursDto workHours = new AttendanceAnomalyDetectionRequestDto.WorkHoursDto();
        workHours.setStart(LocalTime.of(9, 0));
        workHours.setEnd(LocalTime.of(18, 0));
        request.setWorkHours(workHours);
        
        // 设置办公室位置
        AttendanceAnomalyDetectionRequestDto.LocationDto officeLocation = new AttendanceAnomalyDetectionRequestDto.LocationDto();
        officeLocation.setLatitude(39.9042);
        officeLocation.setLongitude(116.4074); // 北京
        request.setOfficeLocation(officeLocation);
        
        AttendanceAnomalyDetectionResponseDto response = attendanceAIService.detectAnomalies("tenant1", request);
        assertNotNull(response);
        assertNotNull(response.getAnomalies());
        // 应该检测到位置异常
        assertTrue(response.getAnomalies().stream().anyMatch(a -> "LOCATION_ANOMALY".equals(a.getAnomalyType())));
    }

    @Test
    public void testApproveLeave() {
        LeaveRequestDto request = new LeaveRequestDto();
        request.setRequestId("req1");
        request.setEmployeeId("emp1");
        request.setLeaveType("PERSONAL");
        request.setDurationDays(1);
        
        LeaveApprovalResponseDto response = attendanceAIService.approveLeave("tenant1", request);
        assertNotNull(response);
        assertNotNull(response.getApprovalStatus());
    }

    @Test
    public void testPredictWorkHours() {
        WorkHourPredictionRequestDto request = new WorkHourPredictionRequestDto();
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(30));
        
        WorkHourPredictionResponseDto response = attendanceAIService.predictWorkHours("tenant1", request);
        assertNotNull(response);
        assertNotNull(response.getForecasts());
        assertTrue(response.getForecasts().size() > 0);
    }
}



