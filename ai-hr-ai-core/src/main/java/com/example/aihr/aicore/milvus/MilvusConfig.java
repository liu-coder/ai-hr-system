package com.example.aihr.aicore.milvus;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.client.ConnectConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MilvusProperties.class)
public class MilvusConfig {

    @Bean(destroyMethod = "close")
    public MilvusClientV2 milvusClientV2(MilvusProperties props) {
        ConnectConfig cfg = ConnectConfig.builder()
                .uri(props.uri())
                .dbName(props.getDatabase())
                .connectTimeoutMs(5_000)
                .enablePrecheck(true)
                .build();
        return new MilvusClientV2(cfg);
    }
}




