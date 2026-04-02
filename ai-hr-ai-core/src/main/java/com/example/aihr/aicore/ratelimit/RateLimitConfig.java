package com.example.aihr.aicore.ratelimit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis 滑动窗口限流实现。
 *
 * 实现说明：
 * - Redis 可用：使用 Lua 脚本实现真正的滑动窗口限流，确保原子性操作
 * - Redis 不可用：内存兜底（进程内），使用滑动窗口算法确保线程安全
 *
 * 约定：
 * - 默认 10 req/s
 * - AI Chat 专用 5 req/s
 */
@Component
public class RateLimitConfig {

    private static final String PREFIX = "ai-hr:rate:";
    private static final int WINDOW_SIZE_SECONDS = 1; // 滑动窗口大小，单位：秒
    private static final int BUCKET_COUNT = 10; // 时间桶数量，用于滑动窗口

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    private final ConcurrentHashMap<String, SlidingWindow> mem = new ConcurrentHashMap<>();
    
    // Lua 脚本，实现滑动窗口限流
    private static final String SLIDING_WINDOW_SCRIPT = """
        local key = KEYS[1]
        local limit = tonumber(ARGV[1])
        local windowMs = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])  -- 统一使用毫秒时间戳
        
        -- 清理过期数据（精确到毫秒）
        redis.call('ZREMRANGEBYSCORE', key, 0, now - windowMs)
        
        -- 直接使用时间戳作为成员（无需序列号）
        if redis.call('ZADD', key, 'NX', now, now) == 1 then
            -- 新增请求时设置过期时间（单位：秒）
            redis.call('EXPIRE', key, math.ceil(windowMs/1000))
        end
        
        -- 精确统计窗口内请求数
        local count = redis.call('ZCOUNT', key, now - windowMs, now)
        return count <= limit
    """;
    
    private final RedisScript<Boolean> slidingWindowScript = RedisScript.of(SLIDING_WINDOW_SCRIPT, Boolean.class);

    private static class TimeBucket {
        long timestamp;
        final AtomicInteger count;
        
        TimeBucket(long timestamp) {
            this.timestamp = timestamp;
            this.count = new AtomicInteger(0);
        }
    }

    private static class SlidingWindow {
        final List<TimeBucket> buckets;
        final AtomicInteger totalCount;
        final int limit;
        final long windowSizeMs;
        final long bucketSizeMs;
        
        SlidingWindow(int limit) {
            this.limit = limit;
            this.windowSizeMs = WINDOW_SIZE_SECONDS * 1000L;
            this.bucketSizeMs = this.windowSizeMs / BUCKET_COUNT;
            this.buckets = new ArrayList<>(BUCKET_COUNT);
            this.totalCount = new AtomicInteger(0);
            
            // 初始化时间桶
            long now = System.currentTimeMillis();
            for (int i = 0; i < BUCKET_COUNT; i++) {
                buckets.add(new TimeBucket(now - (BUCKET_COUNT - 1 - i) * bucketSizeMs));
            }
        }
        
        synchronized boolean tryAcquire() {
            // 清理过期的桶
            cleanupExpiredBuckets();
            
            // 检查是否超过限制
            if (totalCount.get() >= limit) {
                return false;
            }
            
            // 增加当前桶的计数
            long now = System.currentTimeMillis();
            int bucketIndex = getBucketIndex(now);
            buckets.get(bucketIndex).count.incrementAndGet();
            totalCount.incrementAndGet();
            
            return true;
        }
        
        private void cleanupExpiredBuckets() {
            long now = System.currentTimeMillis();
            long cutoffTime = now - windowSizeMs;
            
            for (TimeBucket bucket : buckets) {
                if (bucket.timestamp < cutoffTime) {
                    totalCount.addAndGet(-bucket.count.get());
                    bucket.count.set(0);
                    bucket.timestamp = now - (now % bucketSizeMs) + bucketSizeMs * (buckets.indexOf(bucket) - BUCKET_COUNT);
                }
            }
        }
        
        private int getBucketIndex(long timestamp) {
            return (int) ((timestamp / bucketSizeMs) % BUCKET_COUNT);
        }
    }

    public RateLimitConfig(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redisProvider = redisProvider;
    }

    public boolean tryAcquire(String tenantId, String userId, boolean aiChat) {
        int limit = aiChat ? 5 : 10;
        String bucketKey = tenantId + ":" + (userId == null || userId.isBlank() ? "anonymous" : userId);
        String key = PREFIX + (aiChat ? "ai-chat" : "default") + ":" + bucketKey;
        long currentTime = System.currentTimeMillis(); // 使用毫秒时间戳

        // Redis 滑动窗口限流（使用 Lua 脚本确保原子性）
        try {
            StringRedisTemplate redis = redisProvider.getIfAvailable();
            if (redis != null) {
                Boolean allowed = redis.execute(slidingWindowScript, 
                    java.util.Collections.singletonList(key), 
                    String.valueOf(limit),
                    String.valueOf(WINDOW_SIZE_SECONDS * 1000L), // 转换为毫秒
                    String.valueOf(currentTime));
                return allowed != null && allowed;
            }
        } catch (Exception ignored) {
            // Redis 不可用 -> 内存兜底
        }

        // In-memory fallback (per process) - 使用滑动窗口算法
        String memKey = bucketKey + ":" + (aiChat ? "ai-chat" : "default");
        SlidingWindow window = mem.computeIfAbsent(memKey, k -> new SlidingWindow(limit));
        return window.tryAcquire();
    }
}




