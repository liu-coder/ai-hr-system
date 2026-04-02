package com.example.aihr.aicore.rag;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocumentEntity, String> {
    List<PolicyDocumentEntity> findByTenantIdAndDocTypeAndTitle(String tenantId, String docType, String title);
}




