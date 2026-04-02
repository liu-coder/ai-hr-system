package com.example.aihr.aicore.milvus;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MilvusProperties.class)
public class MilvusConfig {
    // 这里不再直接创建 MilvusClientV2 Bean。
    // 目的：避免 Milvus 短暂不可用时阻断 ai-core 启动与聊天服务可用性。
}




