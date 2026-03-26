package com.example.aihr.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "user_roles")
@IdClass(UserRoleEntity.Pk.class)
public class UserRoleEntity {
    @Id
    @Column(name = "user_id")
    private String userId;

    @Id
    @Column(name = "role")
    private String role;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public static class Pk implements Serializable {
        private String userId;
        private String role;

        public Pk() {}
        public Pk(String userId, String role) {
            this.userId = userId;
            this.role = role;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pk pk = (Pk) o;
            return Objects.equals(userId, pk.userId) && Objects.equals(role, pk.role);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, role);
        }
    }
}

