package com.example.aihr.aicore.milvus;

import io.milvus.v2.client.MilvusClientV2;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MilvusHealthIndicator implements HealthIndicator {
    private final MilvusClientV2 client;
    private final MilvusProperties props;

    public MilvusHealthIndicator(MilvusClientV2 client, MilvusProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public Health health() {
        try {
            // Lightweight call; validates connectivity.
            client.listCollections();
            return Health.up().withDetails(Map.of(
                    "uri", props.uri(),
                    "database", props.getDatabase(),
                    "collection", props.getCollection()
            )).build();
        } catch (Exception e) {
            return Health.down(e).withDetails(Map.of(
                    "uri", props.uri(),
                    "database", props.getDatabase(),
                    "collection", props.getCollection()
            )).build();
        }
    }
}




