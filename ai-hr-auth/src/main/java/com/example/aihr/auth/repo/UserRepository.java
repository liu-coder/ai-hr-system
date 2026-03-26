package com.example.aihr.auth.repo;

import com.example.aihr.auth.domain.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByTenantIdAndUsername(String tenantId, String username);
}

