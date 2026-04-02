package com.example.aihr.aicore.config;

import com.example.aihr.aicore.rag.PolicyRagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * 缓存预热组件，用于在系统启动时预热热点数据。
 * <p>
 * 通过在系统启动时预先加载频繁访问的数据，减少系统运行时的缓存穿透和缓存雪崩问题。
 */
@Component
public class CacheWarmer {
    
    private final PolicyRagService ragService;
    
    public CacheWarmer(PolicyRagService ragService) {
        this.ragService = ragService;
    }
    
    /**
     * 系统启动时执行缓存预热。
     * <p>
     * 预热热点政策数据，包括考勤政策、薪资政策、请假流程、加班政策等。
     */
    @PostConstruct
    public void warmupCache() {
        // 预热热点政策数据
        try {
            List<String> hotTopics = Arrays.asList("考勤政策", "薪资政策", "请假流程", "加班政策", "绩效评估", "培训管理");
            for (String topic : hotTopics) {
                ragService.search("default", topic, 3);
            }
            System.out.println("Cache warmup completed successfully");
        } catch (Exception e) {
            System.err.println("Cache warmup failed: " + e.getMessage());
        }
    }
}
