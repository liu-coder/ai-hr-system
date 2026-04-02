package com.example.aihr.aicore.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 监控服务类，用于管理和记录各种监控指标。
 * <p>
 * 提供了各种监控指标的记录方法，包括计数器、计时器等。
 */
@Service
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    
    /**
     * 构造函数。
     * 
     * @param meterRegistry 指标注册表
     */
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 注册活跃请求数指标
        Gauge.builder("aihr.requests.active", activeRequests, AtomicInteger::get)
                .description("Number of active requests")
                .register(meterRegistry);
    }
    
    /**
     * 记录聊天请求。
     */
    public void recordChatRequest() {
        Counter.builder("aihr.chat.requests")
                .description("Number of chat requests")
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 记录工具调用。
     * 
     * @param toolName 工具名称
     * @param success 是否成功
     */
    public void recordToolCall(String toolName, boolean success) {
        Counter.builder("aihr.tool.calls")
                .tag("tool", toolName)
                .tag("success", String.valueOf(success))
                .description("Number of tool calls")
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 记录工具调用时间。
     * 
     * @param toolName 工具名称
     * @param duration 调用时间（毫秒）
     * @param success 是否成功
     */
    public void recordToolCallDuration(String toolName, long duration, boolean success) {
        Timer.builder("aihr.tool.calls.duration")
                .tag("tool", toolName)
                .tag("success", String.valueOf(success))
                .description("Tool call duration")
                .register(meterRegistry)
                .record(Duration.ofMillis(duration));
    }
    
    /**
     * 记录LLM调用。
     * 
     * @param success 是否成功
     */
    public void recordLlmCall(boolean success) {
        Counter.builder("aihr.llm.calls")
                .tag("success", String.valueOf(success))
                .description("Number of LLM calls")
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 记录LLM调用时间。
     * 
     * @param duration 调用时间（毫秒）
     * @param success 是否成功
     */
    public void recordLlmCallDuration(long duration, boolean success) {
        Timer.builder("aihr.llm.calls.duration")
                .tag("success", String.valueOf(success))
                .description("LLM call duration")
                .register(meterRegistry)
                .record(Duration.ofMillis(duration));
    }
    
    /**
     * 记录错误。
     * 
     * @param errorType 错误类型
     */
    public void recordError(String errorType) {
        Counter.builder("aihr.errors")
                .tag("type", errorType)
                .description("Number of errors")
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 记录缓存命中。
     * 
     * @param cacheName 缓存名称
     */
    public void recordCacheHit(String cacheName) {
        Counter.builder("aihr.cache.hits")
                .tag("cache", cacheName)
                .description("Number of cache hits")
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 记录缓存未命中。
     * 
     * @param cacheName 缓存名称
     */
    public void recordCacheMiss(String cacheName) {
        Counter.builder("aihr.cache.misses")
                .tag("cache", cacheName)
                .description("Number of cache misses")
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 增加活跃请求数。
     */
    public void incrementActiveRequests() {
        activeRequests.incrementAndGet();
    }
    
    /**
     * 减少活跃请求数。
     */
    public void decrementActiveRequests() {
        activeRequests.decrementAndGet();
    }
}
