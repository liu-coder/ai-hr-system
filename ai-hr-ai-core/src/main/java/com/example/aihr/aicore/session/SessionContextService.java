package com.example.aihr.aicore.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

/**
 * 会话级待补全槽位与追问提示（扩展点：智能补参与追问）。
 * P1：优先 Redis（TTL 30 分钟），Redis 不可用时内存兜底（支持多实例）。
 */
@Service
public class SessionContextService {

    private static class SessionEntry {
        final PendingSlots slots;
        final Instant timestamp;
        
        SessionEntry(PendingSlots slots) {
            this.slots = slots;
            this.timestamp = Instant.now();
        }
    }
    
    private final ConcurrentHashMap<String, SessionEntry> store = new ConcurrentHashMap<>();
    private static final int MAX_SESSIONS = 10_000;
    private static final String REDIS_PREFIX = "ai-hr:session:pending:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectMapper om;
    
    // Lua 脚本，确保 get 和 delete 操作的原子性
    private static final String GET_AND_DELETE_SCRIPT = """
        local key = KEYS[1]
        local value = redis.call('get', key)
        if value then
            redis.call('del', key)
        end
        return value
    """;
    
    private final RedisScript<String> getAndDeleteScript = RedisScript.of(GET_AND_DELETE_SCRIPT, String.class);

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

        // 内存兜底，使用原子操作确保线程安全
        if (store.size() >= MAX_SESSIONS) {
            evictOldest();
        }
        store.put(sessionId, new SessionEntry(pending));
    }

    public PendingSlots getAndClear(String sessionId) {
        try {
            StringRedisTemplate redis = redisProvider.getIfAvailable();
            if (redis != null) {
                String json = redis.execute(getAndDeleteScript, java.util.Collections.singletonList(key(sessionId)));
                if (json != null) {
                    return om.readValue(json, PendingSlots.class);
                }
            }
        } catch (Exception ignored) {}
        SessionEntry entry = store.remove(sessionId);
        return entry != null ? entry.slots : null;
    }

    public PendingSlots get(String sessionId) {
        try {
            StringRedisTemplate redis = redisProvider.getIfAvailable();
            if (redis != null) {
                String json = redis.opsForValue().get(key(sessionId));
                if (json != null) {
                    return om.readValue(json, PendingSlots.class);
                }
            }
        } catch (Exception ignored) {}
        SessionEntry entry = store.get(sessionId);
        return entry != null ? entry.slots : null;
    }

    private void evictOldest() {
        // 找出最早的会话并移除
        store.entrySet().stream()
            .min(Map.Entry.comparingByValue((e1, e2) -> e1.timestamp.compareTo(e2.timestamp)))
            .ifPresent(entry -> store.remove(entry.getKey()));
    }

    private String key(String sessionId) {
        return REDIS_PREFIX + sessionId;
    }

    public record PendingSlots(String route, Map<String, Object> extractedSoFar, String followUpPrompt) {}
}



