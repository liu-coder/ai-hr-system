package com.example.aihr.aicore.milvus;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MilvusProperties.class)
public class MilvusConfig {
    // Intentionally no MilvusClientV2 bean here.
    // We avoid a hard startup dependency on Milvus; temporary Milvus outages
    // should not prevent ai-core from booting and serving chat requests.
}




