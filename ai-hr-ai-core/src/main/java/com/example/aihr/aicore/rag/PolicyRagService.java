package com.example.aihr.aicore.rag;

import com.example.aihr.aicore.milvus.MilvusProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyRagService {
    private final PolicyDocumentRepository docs;
    private final PolicyChunkRepository chunks;
    private final EmbeddingService embedding;
    private final VectorStore vectorStore;
    private final MilvusProperties milvusProps;
    private final ExecutorService executorService;

    public PolicyRagService(PolicyDocumentRepository docs,
                            PolicyChunkRepository chunks,
                            EmbeddingService embedding,
                            VectorStore vectorStore,
                            MilvusProperties milvusProps) {
        this.docs = docs;
        this.chunks = chunks;
        this.embedding = embedding;
        this.vectorStore = vectorStore;
        this.milvusProps = milvusProps;
        this.executorService = Executors.newFixedThreadPool(5);
    }

    @Transactional
    public String ingest(String tenantId,
                         String docType,
                         String title,
                         String version,
                         String securityLevel,
                         LocalDate effectiveFrom,
                         LocalDate effectiveTo,
                         String sourceUri,
                         String createdBy,
                         List<String> chunkContents) {
        // 将同一类型和标题的旧文档标记为INACTIVE
        List<PolicyDocumentEntity> oldDocs = docs.findByTenantIdAndDocTypeAndTitle(tenantId, docType, title);
        for (PolicyDocumentEntity oldDoc : oldDocs) {
            oldDoc.setStatus("INACTIVE");
            docs.save(oldDoc);
        }
        
        PolicyDocumentEntity d = new PolicyDocumentEntity();
        d.setId("pd-" + UUID.randomUUID());
        d.setTenantId(tenantId);
        d.setDocType(docType);
        d.setTitle(title);
        d.setVersion(version);
        d.setSecurityLevel(securityLevel);
        d.setEffectiveFrom(effectiveFrom);
        d.setEffectiveTo(effectiveTo);
        d.setSourceUri(sourceUri);
        d.setCreatedAt(Instant.now());
        d.setCreatedBy(createdBy);
        d.setStatus("ACTIVE");
        docs.save(d);

        for (int i = 0; i < chunkContents.size(); i++) {
            String content = chunkContents.get(i);
            String chunkId = "pc-" + UUID.randomUUID();
            // embedding vector for this chunk（用于 Milvus 向量检索；当前实现是伪 embedding）
            List<Float> vec = embedding.embed(content);

            PolicyChunkEntity c = new PolicyChunkEntity();
            c.setId(chunkId);
            c.setTenantId(tenantId);
            c.setDocumentId(d.getId());
            c.setChunkIndex(i);
            c.setContent(content);
            c.setMetadataJson(toMetaJson(docType, title, version, securityLevel, effectiveFrom, effectiveTo, sourceUri, i));
            c.setEmbeddingJson(null);
            c.setCreatedAt(Instant.now());
            chunks.save(c);

            // Best-effort upsert to Milvus (if available) - asynchronous
            if (vectorStore.isAvailable()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        vectorStore.upsert(tenantId, chunkId, vec);
                    } catch (Exception e) {
                        // Log error but don't fail the ingest process
                        System.err.println("Failed to upsert to Milvus: " + e.getMessage());
                    }
                }, executorService);
            }
        }
        return d.getId();
    }

    @Cacheable(value = "policySearch", key = "#tenantId + ':' + #query + ':' + #topK")
    public SearchResult search(String tenantId, String query, int topK) {
        List<String> ids = List.of();
        if (vectorStore.isAvailable()) {
            ids = vectorStore.search(tenantId, embedding.embed(query), topK);
        }
        List<PolicyChunkEntity> hitChunks;
        if (ids != null && !ids.isEmpty()) {
            hitChunks = chunks.findAllById(ids);
        } else {
            // Fallback: keyword search
            String kw = query.length() > 16 ? query.substring(0, 16) : query;
            hitChunks = chunks.searchByKeyword(tenantId, kw);
            if (hitChunks.size() > topK) hitChunks = hitChunks.subList(0, topK);
        }
        return new SearchResult(query, vectorStore.isAvailable(), milvusProps.uri(), hitChunks.stream()
                .map(c -> new Hit(c.getId(), c.getDocumentId(), c.getChunkIndex(), c.getContent(), c.getMetadataJson()))
                .toList());
    }

    private String toMetaJson(String docType, String title, String version, String securityLevel,
                              LocalDate effectiveFrom, LocalDate effectiveTo, String sourceUri, int chunkIndex) {
        // minimal JSON without additional dependencies
        return "{\"docType\":\"" + esc(docType) + "\"," +
                "\"title\":\"" + esc(title) + "\"," +
                "\"version\":\"" + esc(version) + "\"," +
                "\"securityLevel\":\"" + esc(securityLevel) + "\"," +
                "\"effectiveFrom\":\"" + effectiveFrom + "\"," +
                "\"effectiveTo\":\"" + (effectiveTo == null ? "" : effectiveTo) + "\"," +
                "\"sourceUri\":\"" + esc(sourceUri) + "\"," +
                "\"chunkIndex\":" + chunkIndex +
                "}";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record SearchResult(String query, boolean milvusAvailable, String milvusUri, List<Hit> hits) {}
    public record Hit(String chunkId, String documentId, int chunkIndex, String content, String metadataJson) {}
}




