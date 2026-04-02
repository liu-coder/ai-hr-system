package com.example.aihr.aicore.milvus;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import jakarta.annotation.PreDestroy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class MilvusClientHolder {
    private final MilvusProperties props;
    private final AtomicReference<MilvusClientV2> clientRef = new AtomicReference<>();
    private final AtomicBoolean initTried = new AtomicBoolean(false);

    public MilvusClientHolder(MilvusProperties props) {
        this.props = props;
    }

    public Optional<MilvusClientV2> getOptional() {
        MilvusClientV2 existing = clientRef.get();
        if (existing != null) {
            return Optional.of(existing);
        }
        if (initTried.get()) {
            return Optional.empty();
        }
        synchronized (clientRef) {
            existing = clientRef.get();
            if (existing != null) {
                return Optional.of(existing);
            }
            initTried.set(true);
            try {
                // Lazy init: only connect when the vector store is actually used.
                // If Milvus is down, we fail closed and keep ai-core running.
                ConnectConfig cfg = ConnectConfig.builder()
                        .uri(props.uri())
                        .dbName(props.getDatabase())
                        .connectTimeoutMs(5_000)
                        .enablePrecheck(true)
                        .build();
                MilvusClientV2 created = new MilvusClientV2(cfg);
                clientRef.set(created);
                return Optional.of(created);
            } catch (Exception e) {
                System.err.println("Milvus client init failed: " + e.getMessage());
                return Optional.empty();
            }
        }
    }

    @PreDestroy
    public void close() {
        MilvusClientV2 client = clientRef.get();
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {
                // Best-effort close
            }
        }
    }
}
