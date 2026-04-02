package com.example.aihr.aicore.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型监控服务，提供AI模型的实时监控和评估功能。
 */
@Service
public class ModelMonitoringService {

    private final Map<String, ModelMetrics> modelMetricsMap = new ConcurrentHashMap<>();
    private final Map<String, List<ModelPerformanceRecord>> performanceHistory = new ConcurrentHashMap<>();

    /**
     * 记录模型预测
     * @param tenantId 租户ID
     * @param modelType 模型类型
     * @param input 输入数据
     * @param output 输出数据
     * @param latencyMs 延迟时间（毫秒）
     * @param isCorrect 是否正确
     */
    public void recordPrediction(String tenantId, String modelType, Object input, Object output, long latencyMs, boolean isCorrect) {
        String modelKey = tenantId + ":" + modelType;
        ModelMetrics metrics = modelMetricsMap.computeIfAbsent(modelKey, k -> new ModelMetrics());

        // 更新实时指标
        metrics.totalPredictions.incrementAndGet();
        if (isCorrect) {
            metrics.correctPredictions.incrementAndGet();
        }
        metrics.totalLatency.addAndGet(latencyMs);

        // 记录性能历史
        ModelPerformanceRecord record = new ModelPerformanceRecord(
                Instant.now(),
                latencyMs,
                isCorrect
        );
        performanceHistory.computeIfAbsent(modelKey, k -> new ArrayList<>()).add(record);

        // 保持历史记录不超过1000条
        List<ModelPerformanceRecord> history = performanceHistory.get(modelKey);
        if (history.size() > 1000) {
            history.subList(0, history.size() - 1000).clear();
        }
    }

    /**
     * 获取模型指标
     * @param tenantId 租户ID
     * @param modelType 模型类型
     * @return 模型指标
     */
    public ModelMetrics getModelMetrics(String tenantId, String modelType) {
        String modelKey = tenantId + ":" + modelType;
        return modelMetricsMap.getOrDefault(modelKey, new ModelMetrics());
    }

    /**
     * 获取模型性能历史
     * @param tenantId 租户ID
     * @param modelType 模型类型
     * @param limit 限制数量
     * @return 性能历史记录
     */
    public List<ModelPerformanceRecord> getPerformanceHistory(String tenantId, String modelType, int limit) {
        String modelKey = tenantId + ":" + modelType;
        List<ModelPerformanceRecord> history = performanceHistory.getOrDefault(modelKey, new ArrayList<>());
        int startIndex = Math.max(0, history.size() - limit);
        return history.subList(startIndex, history.size());
    }

    /**
     * 计算模型健康状态
     * @param tenantId 租户ID
     * @param modelType 模型类型
     * @return 健康状态
     */
    public ModelHealthStatus calculateHealthStatus(String tenantId, String modelType) {
        String modelKey = tenantId + ":" + modelType;
        ModelMetrics metrics = modelMetricsMap.getOrDefault(modelKey, new ModelMetrics());
        List<ModelPerformanceRecord> history = performanceHistory.getOrDefault(modelKey, new ArrayList<>());

        // 计算准确率
        double accuracy = metrics.totalPredictions.get() > 0 ? 
                (double) metrics.correctPredictions.get() / metrics.totalPredictions.get() : 0.0;

        // 计算平均延迟
        double avgLatency = metrics.totalPredictions.get() > 0 ? 
                (double) metrics.totalLatency.get() / metrics.totalPredictions.get() : 0.0;

        // 计算最近100次预测的准确率
        double recentAccuracy = 0.0;
        if (history.size() > 0) {
            int recentCount = Math.min(100, history.size());
            List<ModelPerformanceRecord> recentHistory = history.subList(history.size() - recentCount, history.size());
            long recentCorrect = recentHistory.stream().filter(ModelPerformanceRecord::isCorrect).count();
            recentAccuracy = (double) recentCorrect / recentCount;
        }

        // 确定健康状态
        String status;
        if (accuracy < 0.8 || recentAccuracy < 0.7) {
            status = "UNHEALTHY";
        } else if (accuracy < 0.9 || recentAccuracy < 0.85) {
            status = "WARNING";
        } else {
            status = "HEALTHY";
        }

        return new ModelHealthStatus(
                modelType,
                status,
                accuracy,
                avgLatency,
                recentAccuracy,
                metrics.totalPredictions.get(),
                Instant.now()
        );
    }

    /**
     * 模型指标
     */
    public static class ModelMetrics {
        public final java.util.concurrent.atomic.AtomicInteger totalPredictions = new java.util.concurrent.atomic.AtomicInteger(0);
        public final java.util.concurrent.atomic.AtomicInteger correctPredictions = new java.util.concurrent.atomic.AtomicInteger(0);
        public final java.util.concurrent.atomic.AtomicLong totalLatency = new java.util.concurrent.atomic.AtomicLong(0);

        public double getAccuracy() {
            return totalPredictions.get() > 0 ? 
                    (double) correctPredictions.get() / totalPredictions.get() : 0.0;
        }

        public double getAverageLatency() {
            return totalPredictions.get() > 0 ? 
                    (double) totalLatency.get() / totalPredictions.get() : 0.0;
        }
    }

    /**
     * 模型性能记录
     */
    public record ModelPerformanceRecord(
            Instant timestamp,
            long latencyMs,
            boolean isCorrect
    ) {}

    /**
     * 模型健康状态
     */
    public record ModelHealthStatus(
            String modelType,
            String status, // HEALTHY, WARNING, UNHEALTHY
            double accuracy,
            double averageLatencyMs,
            double recentAccuracy,
            int totalPredictions,
            Instant lastUpdated
    ) {}
}
