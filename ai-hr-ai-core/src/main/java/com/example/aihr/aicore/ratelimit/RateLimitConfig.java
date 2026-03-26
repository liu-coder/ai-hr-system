package com.example.aihr.aicore.ratelimit;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 令牌桶/限流（P1）。
 *
 * 实现说明（当前实现取“每秒窗口限流”的近似形态）：
 * - Redis 可用：INCR + TTL 控制每秒请求上限
 * - Redis 不可用：内存兜底（进程内）
 *
 * 约定：
 * - 默认 10 req/s
 * - AI Chat 专用 5 req/s
 */
@Component
public class RateLimitConfig {

    private static final String PREFIX = "ai-hr:rate:";

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    private final ConcurrentHashMap<String, MemWindow> mem = new ConcurrentHashMap<>();

    private record MemWindow(long second, int count) {}

    public RateLimitConfig(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redisProvider = redisProvider;
    }

    public boolean tryAcquire(String tenantId, String userId, boolean aiChat) {
        int limit = aiChat ? 5 : 10;
        String bucketKey = tenantId + ":" + (userId == null || userId.isBlank() ? "anonymous" : userId);
        long sec = Instant.now().getEpochSecond();

        // Redis fixed-window per second (approx token bucket)
        String redisKey = PREFIX + (aiChat ? "ai-chat" : "default") + ":" + bucketKey + ":" + sec;
        try {
            StringRedisTemplate redis = redisProvider.getIfAvailable();
            if (redis != null) {
                Long c = redis.opsForValue().increment(redisKey);
                if (c != null && c == 1L) {
                    // key 自然失效，避免累积
                    redis.expire(redisKey, java.time.Duration.ofSeconds(2));
                }
                if (c == null) return true;
                return c <= limit;
            }
        } catch (Exception ignored) {
            // Redis 不可用 -> 内存兜底
        }

        // In-memory fallback (per process)
        String memKey = bucketKey + ":" + (aiChat ? "ai-chat" : "default");
        MemWindow w = mem.get(memKey);
        if (w == null || w.second != sec) {
            mem.put(memKey, new MemWindow(sec, 1));
            return limit >= 1;
        }
        MemWindow next = new MemWindow(w.second, w.count + 1);
        mem.put(memKey, next);
        return next.count <= limit;
    }
}

