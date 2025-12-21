package com.personal.tracker.auth.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public class UserEntity implements Persistable<UUID> {

        @Id
        private UUID id;

        @Column("username")
        private String username;

        @Column("email")
        private String email;

        @Column("password_hash")
        private String passwordHash;

        @Column("roles")
        private String roles;

        @Column("created_at")
        private Instant createdAt;

        @Transient
        private boolean isNew = false;

        public UserEntity() {
        }

        public UserEntity(UUID id, String username, String passwordHash, String roles, Instant createdAt) {
                this.id = id;
                this.username = username;
                this.passwordHash = passwordHash;
                this.roles = roles;
                this.createdAt = createdAt;
        }

        @Override
        public UUID getId() {
                return id;
        }

        public void setId(UUID id) {
                this.id = id;
        }

        public String getUsername() {
                return username;
        }

        public void setUsername(String username) {
                this.username = username;
        }

        public String getEmail() {
                return email;
        }

        public void setEmail(String email) {
                this.email = email;
        }

        public String getPasswordHash() {
                return passwordHash;
        }

        public void setPasswordHash(String passwordHash) {
                this.passwordHash = passwordHash;
        }

        public String getRoles() {
                return roles;
        }

        public void setRoles(String roles) {
                this.roles = roles;
        }

        public Instant getCreatedAt() {
                return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
                this.createdAt = createdAt;
        }

        public void setNewEntity(boolean isNew) {
                this.isNew = isNew;
        }

        @Override
        public boolean isNew() {
                return isNew || id == null;
        }
}
