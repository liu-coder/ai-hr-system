package com.example.aihr.aicore.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 会话级待补全槽位与追问提示（扩展点：智能补参与追问）。
 * P1：优先 Redis（TTL 30 分钟），Redis 不可用时内存兜底（支持多实例）。
 */
@Service
public class SessionContextService {

    private final Map<String, PendingSlots> store = new ConcurrentHashMap<>();
    private static final int MAX_SESSIONS = 10_000;
    private static final String REDIS_PREFIX = "ai-hr:session:pending:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectMapper om;

    public SessionContextService(ObjectProvider<StringRedisTemplate> redisProvider, ObjectMapper om) {
        this.redisProvider = redisProvider;
        this.om = om;
    }

    public void setPending(String sessionId, String route, Map<String, Object> extractedSoFar, String followUpPrompt) {
        PendingSlots pending = new PendingSlots(route, extractedSoFar, followUpPrompt);
        String key = key(sessionId);

        try {
            StringRedisTemplate redis = redisProvider.getIfAvailable();
            if (redis != null) {
                redis.opsForValue().set(key, om.writeValueAsString(pending), TTL);
                return;
            }
        } catch (Exception ignored) {
            // Redis 不可用时走内存兜底
        }

        if (store.size() >= MAX_SESSIONS) evictOne();
        store.put(sessionId, pending);
    }

    public PendingSlots getAndClear(String sessionId) {
        StringRedisTemplate redis = null;
        try {
            redis = redisProvider.getIfAvailable();
            if (redis != null) {
                String json = redis.opsForValue().get(key(sessionId));
                if (json != null) {
                    redis.delete(key(sessionId));
                    return om.readValue(json, PendingSlots.class);
                }
            }
        } catch (Exception ignored) {}
        return store.remove(sessionId);
    }

    public PendingSlots get(String sessionId) {
        StringRedisTemplate redis = null;
        try {
            redis = redisProvider.getIfAvailable();
            if (redis != null) {
                String json = redis.opsForValue().get(key(sessionId));
                if (json != null) {
                    return om.readValue(json, PendingSlots.class);
                }
            }
        } catch (Exception ignored) {}
        return store.get(sessionId);
    }

    private void evictOne() {
        store.keySet().stream().findFirst().ifPresent(store::remove);
    }

    private String key(String sessionId) {
        return REDIS_PREFIX + sessionId;
    }

    public record PendingSlots(String route, Map<String, Object> extractedSoFar, String followUpPrompt) {}
}
