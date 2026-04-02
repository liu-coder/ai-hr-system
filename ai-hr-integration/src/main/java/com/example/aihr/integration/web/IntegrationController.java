package com.example.aihr.integration.web;

import com.example.aihr.common.security.RequestContext;
import com.example.aihr.common.security.RequestContextHolder;
import com.example.aihr.integration.service.IntegrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 集成控制器
 * 处理与外部系统集成相关的HTTP请求，包括ERP和CRM系统的员工数据同步和查询
 */
@RestController
@RequestMapping(path = "/v1/integration", produces = MediaType.APPLICATION_JSON_VALUE)
public class IntegrationController {
    private final IntegrationService integrationService;

    /**
     * 构造函数
     * 
     * @param integrationService 集成服务
     */
    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    /**
     * 与ERP系统同步员工数据
     * @param req 员工数据
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return 同步结果
     */
    @PostMapping(path = "/erp/sync", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> syncWithERP(@Valid @RequestBody Map<String, Object> req, HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();

        return integrationService.syncWithERP(tenantId, req);
    }

    /**
     * 与CRM系统同步员工数据
     * @param req 员工数据
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return 同步结果
     */
    @PostMapping(path = "/crm/sync", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> syncWithCRM(@Valid @RequestBody Map<String, Object> req, HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();

        return integrationService.syncWithCRM(tenantId, req);
    }

    /**
     * 从ERP系统获取员工数据
     * @param employeeId 员工ID
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return 员工数据
     */
    @GetMapping(path = "/erp/employee")
    public Map<String, Object> getEmployeeFromERP(@RequestParam @NotBlank String employeeId, HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();

        return integrationService.getEmployeeFromERP(tenantId, employeeId);
    }

    /**
     * 从CRM系统获取员工数据
     * @param employeeId 员工ID
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return 员工数据
     */
    @GetMapping(path = "/crm/employee")
    public Map<String, Object> getEmployeeFromCRM(@RequestParam @NotBlank String employeeId, HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();

        return integrationService.getEmployeeFromCRM(tenantId, employeeId);
    }
}
