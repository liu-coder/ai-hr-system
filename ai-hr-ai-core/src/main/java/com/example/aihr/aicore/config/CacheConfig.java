package com.example.aihr.aicore.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import java.time.Duration;

/**
 * 缓存配置类，启用Spring的缓存功能。
 * <p>
 * 配置了基于Redis的缓存管理器和基于Caffeine的本地缓存，实现多级缓存策略。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 创建基于Redis的缓存管理器，用于分布式缓存。
     * <p>
     * 目前配置了以下缓存：
     * - policySearch：用于缓存政策查询结果
     * - chatResponse：用于缓存聊天响应
     * 
     * @param redisConnectionFactory Redis连接工厂
     * @return Redis缓存管理器
     */
    @Bean("redisCacheManager")
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("aihr:cache:");
        
        return RedisCacheManager.builder(redisConnectionFactory)
                .withCacheConfiguration("policySearch", redisCacheConfiguration)
                .withCacheConfiguration("chatResponse", redisCacheConfiguration.entryTtl(Duration.ofMinutes(10)))
                .build();
    }
    
    /**
     * 创建基于Caffeine的本地缓存管理器，作为二级缓存。
     * <p>
     * 用于缓存热点数据，提高访问速度。
     * 
     * @return 本地缓存管理器
     */
    @Bean("localCacheManager")
    public CaffeineCacheManager localCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("localPolicySearch");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(1000));
        return cacheManager;
    }
}



