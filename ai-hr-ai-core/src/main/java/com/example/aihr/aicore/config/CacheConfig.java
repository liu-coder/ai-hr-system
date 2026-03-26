package com.example.aihr.aicore.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置类，启用Spring的缓存功能。
 * <p>
 * 配置了基于内存的缓存管理器，用于缓存政策查询结果等频繁访问的数据。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 创建缓存管理器，配置需要缓存的缓存名称。
     * <p>
     * 目前配置了以下缓存：
     * - policySearch：用于缓存政策查询结果
     * 
     * @return 缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                "policySearch"
        );
        return cacheManager;
    }
}
