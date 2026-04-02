package com.example.aihr.aicore.milvus;

import java.util.Map;
import java.util.Optional;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MilvusHealthIndicator implements HealthIndicator {
    private final MilvusClientHolder clientHolder;
    private final MilvusProperties props;

    public MilvusHealthIndicator(MilvusClientHolder clientHolder, MilvusProperties props) {
        this.clientHolder = clientHolder;
        this.props = props;
    }

    @Override
    public Health health() {
        try {
            // Lightweight call; validates connectivity.
            Optional<io.milvus.v2.client.MilvusClientV2> clientOpt = clientHolder.getOptional();
            if (clientOpt.isEmpty()) {
                // 仅标记为 DOWN，不影响应用启动。
                return Health.down().withDetails(Map.of(
                        "uri", props.uri(),
                        "database", props.getDatabase(),
                        "collection", props.getCollection(),
                        "reason", "Milvus client unavailable"
                )).build();
            }
            clientOpt.get().listCollections();
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




