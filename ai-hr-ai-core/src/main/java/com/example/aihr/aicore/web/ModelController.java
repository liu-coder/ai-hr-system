package com.example.aihr.aicore.web;

import com.example.aihr.aicore.service.ModelFineTuningService;
import com.example.aihr.aicore.service.ModelMonitoringService;
import com.example.aihr.common.security.RequestContext;
import com.example.aihr.common.security.RequestContextHolder;
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
 * 模型控制器
 * 处理AI模型相关的HTTP请求，包括模型微调和监控等功能
 */
@RestController
@RequestMapping(path = "/v1/model", produces = MediaType.APPLICATION_JSON_VALUE)
public class ModelController {
    private final ModelFineTuningService modelFineTuningService;
    private final ModelMonitoringService modelMonitoringService;

    /**
     * 构造函数
     * 
     * @param modelFineTuningService 模型微调服务
     * @param modelMonitoringService 模型监控服务
     */
    public ModelController(ModelFineTuningService modelFineTuningService, ModelMonitoringService modelMonitoringService) {
        this.modelFineTuningService = modelFineTuningService;
        this.modelMonitoringService = modelMonitoringService;
    }

    /**
     * 微调模型
     * @param modelType 模型类型
     * @param trainingData 训练数据
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return 微调结果
     */
    @PostMapping(path = "/fine-tune", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ModelFineTuningService.FineTuningResult fineTuneModel(
            @RequestParam @NotBlank String modelType,
            @Valid @RequestBody Map<String, Object> trainingData,
            HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();

        return modelFineTuningService.fineTuneModel(tenantId, modelType, trainingData);
    }

    /**
     * 获取模型指标
     * @param modelType 模型类型
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return 模型指标
     */
    @GetMapping(path = "/metrics")
    public ModelMonitoringService.ModelMetrics getModelMetrics(
            @RequestParam @NotBlank String modelType,
            HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();

        return modelMonitoringService.getModelMetrics(tenantId, modelType);
    }

    /**
     * 获取模型性能历史
     * @param modelType 模型类型
     * @param limit 限制数量
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return 性能历史记录
     */
    @GetMapping(path = "/performance-history")
    public java.util.List<ModelMonitoringService.ModelPerformanceRecord> getPerformanceHistory(
            @RequestParam @NotBlank String modelType,
            @RequestParam(defaultValue = "100") int limit,
            HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();

        return modelMonitoringService.getPerformanceHistory(tenantId, modelType, limit);
    }

    /**
     * 获取模型健康状态
     * @param modelType 模型类型
     * @param http 用于读取 `X-Trace-Id`（审计日志关联用）
     * @return 健康状态
     */
    @GetMapping(path = "/health")
    public ModelMonitoringService.ModelHealthStatus getModelHealthStatus(
            @RequestParam @NotBlank String modelType,
            HttpServletRequest http) {
        RequestContext ctx = RequestContextHolder.getRequired();
        String tenantId = ctx.tenantId();

        return modelMonitoringService.calculateHealthStatus(tenantId, modelType);
    }
}
