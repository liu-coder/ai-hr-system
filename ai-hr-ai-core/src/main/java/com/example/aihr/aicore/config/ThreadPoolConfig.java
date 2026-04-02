package com.example.aihr.aicore.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.*;

/**
 * 线程池配置类，用于配置系统中使用的线程池。
 * <p>
 * 配置了可动态调整的线程池，提高系统的并发处理能力。
 */
@Configuration
public class ThreadPoolConfig {
    
    /**
     * 创建聊天服务线程池。
     * <p>
     * 线程池参数根据系统资源动态调整，提高系统的并发处理能力。
     * 
     * @return 聊天服务线程池
     */
    @Bean(name = "chatExecutorService")
    public ExecutorService chatExecutorService() {
        int corePoolSize = Math.max(10, Runtime.getRuntime().availableProcessors() * 2);
        int maxPoolSize = corePoolSize * 2;
        
        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadFactoryBuilder().setNameFormat("chat-executor-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    /**
     * 创建线程池监控组件。
     * <p>
     * 用于监控线程池的运行状态，包括线程池大小、队列长度等指标。
     * 
     * @param meterRegistry 指标注册表
     * @return 线程池监控组件
     */
    @Bean
    public ThreadPoolMonitor threadPoolMonitor(MeterRegistry meterRegistry) {
        return new ThreadPoolMonitor(meterRegistry);
    }
    
    /**
     * 线程池监控类，用于监控线程池的运行状态。
     */
    public static class ThreadPoolMonitor {
        
        private final MeterRegistry meterRegistry;
        
        public ThreadPoolMonitor(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }
        
        /**
         * 监控线程池状态。
         * 
         * @param executorService 线程池
         * @param poolName 线程池名称
         */
        public void monitor(ExecutorService executorService, String poolName) {
            if (executorService instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor pool = (ThreadPoolExecutor) executorService;
                
                // 监控线程池大小
                Gauge.builder("aihr.threadpool.size", pool, ThreadPoolExecutor::getPoolSize)
                        .tag("pool", poolName)
                        .register(meterRegistry);
                
                // 监控活跃线程数
                Gauge.builder("aihr.threadpool.active", pool, ThreadPoolExecutor::getActiveCount)
                        .tag("pool", poolName)
                        .register(meterRegistry);
                
                // 监控队列长度
                Gauge.builder("aihr.threadpool.queue.size", pool.getQueue(), BlockingQueue::size)
                        .tag("pool", poolName)
                        .register(meterRegistry);
                
                // 监控队列剩余容量
                Gauge.builder("aihr.threadpool.queue.remaining", pool.getQueue(), BlockingQueue::remainingCapacity)
                        .tag("pool", poolName)
                        .register(meterRegistry);
            }
        }
    }
}
