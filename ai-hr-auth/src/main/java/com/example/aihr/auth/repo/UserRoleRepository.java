package com.example.aihr.auth.repo;

import com.example.aihr.auth.domain.UserRoleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleEntity.Pk> {
    @Query("select ur.role from UserRoleEntity ur where ur.userId = :userId")
    List<String> findRolesByUserId(@Param("userId") String userId);
}

