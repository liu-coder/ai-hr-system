package com.example.aihr.aicore.rag;

import java.util.List;

public interface VectorStore {
    void upsert(String tenantId, String chunkId, List<Float> vector);
    List<String> search(String tenantId, List<Float> queryVector, int topK);
    boolean isAvailable();
}




