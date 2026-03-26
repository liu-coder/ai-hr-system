package com.example.aihr.aicore.rag;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;
import com.example.aihr.common.exception.AiHrBusinessException;
import com.example.aihr.common.web.ApiError.ErrorCode;

@Component
public class MilvusVectorStore implements VectorStore {
    private final MilvusClientV2 client;
    private final EmbeddingService embeddingService;
    private final io.milvus.v2.client.ConnectConfig connectConfig;
    private final com.example.aihr.aicore.milvus.MilvusProperties props;
    private final AtomicBoolean ensured = new AtomicBoolean(false);

    public MilvusVectorStore(MilvusClientV2 client,
                             com.example.aihr.aicore.milvus.MilvusProperties props,
                             EmbeddingService embeddingService) {
        this.client = client;
        this.props = props;
        this.embeddingService = embeddingService;
        this.connectConfig = null;
    }

    @Override
    public void upsert(String tenantId, String chunkId, List<Float> vector) {
        // P0: 参数验证
        if (!isValidTenantId(tenantId)) {
            throw new AiHrBusinessException(ErrorCode.PARAM_INVALID_ARGUMENT, "无效的租户 ID 格式");
        }
        if (chunkId == null || chunkId.isBlank()) {
            throw new AiHrBusinessException(ErrorCode.PARAM_INVALID_ARGUMENT, "chunkId 不能为空");
        }
        ensureCollection();
        JsonObject row = new JsonObject();
        row.addProperty("chunkId", chunkId);
        row.addProperty("tenantId", tenantId);
        JsonArray arr = new JsonArray();
        for (Float f : vector) arr.add(f);
        row.add("vector", arr);

        UpsertReq req = UpsertReq.builder()
                .databaseName(props.getDatabase())
                .collectionName(props.getCollection())
                .data(List.of(row))
                .build();
        client.upsert(req);
    }

    @Override
    public List<String> search(String tenantId, List<Float> queryVector, int topK) {
        // P0: 参数验证（防止 SQL 注入）
        if (!isValidTenantId(tenantId)) {
            throw new AiHrBusinessException(ErrorCode.PARAM_INVALID_ARGUMENT, "无效的租户 ID 格式");
        }
        
        ensureCollection();

        FloatVec q = new FloatVec(queryVector);
        
        // P0: 使用白名单验证 + 严格转义
        String filter = buildSafeFilter(tenantId);
        SearchReq req = SearchReq.builder()
                .databaseName(props.getDatabase())
                .collectionName(props.getCollection())
                .annsField("vector")
                .metricType(IndexParam.MetricType.COSINE)
                .topK(Math.max(1, topK))
                .filter(filter)
                .outputFields(List.of("chunkId"))
                .data(List.of(q))
                .searchParams(Map.of("nprobe", 16))
                .build();

        SearchResp resp = client.search(req);
        return extractChunkIds(resp, topK);
    }

    @Override
    public boolean isAvailable() {
        try {
            client.listCollections();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void ensureCollection() {
        if (ensured.get()) return;
        synchronized (ensured) {
            if (ensured.get()) return;
            if (!isAvailable()) return;

            Boolean exists = client.hasCollection(HasCollectionReq.builder()
                    .databaseName(props.getDatabase())
                    .collectionName(props.getCollection())
                    .build());
            if (exists == null || !exists) {
                CreateCollectionReq req = CreateCollectionReq.builder()
                        .databaseName(props.getDatabase())
                        .collectionName(props.getCollection())
                        .description("AI HR policy chunks vector store")
                        .dimension(EmbeddingService.DIM)
                        .primaryFieldName("chunkId")
                        .idType(DataType.VarChar)
                        .maxLength(128)
                        .vectorFieldName("vector")
                        .metricType("COSINE")
                        .autoID(false)
                        .enableDynamicField(true)
                        .numShards(2)
                        .build();
                client.createCollection(req);
            }

            client.loadCollection(LoadCollectionReq.builder()
                    .databaseName(props.getDatabase())
                    .collectionName(props.getCollection())
                    .build());
            ensured.set(true);
        }
    }

    private String buildSafeFilter(String tenantId) {
        // 只允许字母、数字、下划线、中划线
        if (!tenantId.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Invalid tenantId format: " + tenantId);
        }
        
        // 使用双引号包裹（Milvus 表达式语法）
        return "tenantId == \"" + tenantId + "\"";
    }
    
    /**
     * 验证租户 ID 格式
     */
    private boolean isValidTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }
        return tenantId.matches("^[a-zA-Z0-9_-]+$");
    }

    private List<String> extractChunkIds(SearchResp resp, int topK) {
        List<String> out = new ArrayList<>();
        if (resp == null || resp.getSearchResults() == null) return out;
        for (List<?> row : resp.getSearchResults()) {
            if (row == null) continue;
            for (Object sr : row) {
                if (sr == null) continue;
                String id = tryExtractId(sr);
                if (id != null && !id.isBlank()) out.add(id);
                if (out.size() >= topK) return out;
            }
        }
        return out;
    }

    private String tryExtractId(Object searchResult) {
        // Best-effort across SDK variations: try getEntity()->Map or getId()
        try {
            // 兼容部分 SDK：searchResult 可能暴露 getEntity()（并且 entity 里包含 chunkId）
            java.lang.reflect.Method m = searchResult.getClass().getMethod("getEntity");
            Object entity = m.invoke(searchResult);
            if (entity instanceof Map<?, ?> map) {
                Object v = map.get("chunkId");
                if (v != null) return String.valueOf(v);
            }
        } catch (Exception ignored) {}

        try {
            // 兼容另一类 SDK：searchResult 可能直接提供 getId()
            java.lang.reflect.Method m = searchResult.getClass().getMethod("getId");
            Object v = m.invoke(searchResult);
            if (v != null) return String.valueOf(v);
        } catch (Exception ignored) {}

        // Fallback: parse toString
        String s = String.valueOf(searchResult);
        int idx = s.indexOf("chunkId");
        if (idx >= 0) return s.substring(idx).replaceAll(".*chunkId[^=]*=([^,}\\]]+).*", "$1").replace("\"", "").trim();
        return null;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

