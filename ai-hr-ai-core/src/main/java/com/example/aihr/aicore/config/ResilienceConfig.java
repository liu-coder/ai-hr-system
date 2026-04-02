package com.example.aihr.aicore.config;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4jBulkheadProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

/**
 * Resilience4j 熔断器配置
 * <p>
 * 配置了熔断、降级和舱壁机制，提高系统的稳定性和可靠性。
 */
@Configuration
public class ResilienceConfig {
    
    /**
     * 配置CircuitBreakerRegistry，用于创建和管理熔断器。
     * 
     * @return CircuitBreakerRegistry
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 失败率阈值，超过此值将触发熔断
                .waitDurationInOpenState(Duration.ofSeconds(60)) // 熔断状态持续时间
                .slidingWindowSize(10) // 滑动窗口大小
                .minimumNumberOfCalls(5) // 最小调用次数
                .build();
        
        return CircuitBreakerRegistry.of(config);
    }
    
    /**
     * 配置TimeLimiterRegistry，用于设置超时时间。
     * 
     * @return TimeLimiterRegistry
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5)) // 超时时间
                .build();
        
        return TimeLimiterRegistry.of(config);
    }
    
    /**
     * 配置Resilience4JCircuitBreakerFactory，用于创建熔断器实例。
     * 
     * @param circuitBreakerRegistry 熔断器注册表
     * @param timeLimiterRegistry 时间限制器注册表
     * @param bulkheadProvider 舱壁提供者
     * @return Resilience4JCircuitBreakerFactory
     */
    @Bean
    public Resilience4JCircuitBreakerFactory resilience4JCircuitBreakerFactory(
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry,
            Resilience4jBulkheadProvider bulkheadProvider) {
        return new Resilience4JCircuitBreakerFactory(circuitBreakerRegistry, timeLimiterRegistry, bulkheadProvider);
    }
    
    /**
     * 配置BulkheadRegistry，用于创建和管理舱壁。
     * 
     * @return BulkheadRegistry
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        return BulkheadRegistry.ofDefaults();
    }
    
    /**
     * 配置ThreadPoolBulkheadRegistry，用于创建和管理线程池舱壁。
     * 
     * @return ThreadPoolBulkheadRegistry
     */
    @Bean
    public ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry() {
        return ThreadPoolBulkheadRegistry.ofDefaults();
    }
}



