package com.example.aihr.aicore.rag;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyChunkRepository extends JpaRepository<PolicyChunkEntity, String> {
    @Query("select c from PolicyChunkEntity c where c.tenantId = :tenantId and c.content like %:kw%")
    List<PolicyChunkEntity> searchByKeyword(@Param("tenantId") String tenantId, @Param("kw") String kw);
    
    @Query("select c from PolicyChunkEntity c where c.tenantId = :tenantId and c.documentId = :documentId")
    List<PolicyChunkEntity> findByTenantIdAndDocumentId(@Param("tenantId") String tenantId, @Param("documentId") String documentId);
}




