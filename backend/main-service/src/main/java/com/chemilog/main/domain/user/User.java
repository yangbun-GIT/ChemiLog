package com.chemilog.main.domain.user;

import com.chemilog.main.domain.common.SoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
public class User extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "health_profile", columnDefinition = "jsonb")
    private Map<String, Object> healthProfile = new HashMap<>();

    protected User() {
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Map<String, Object> getHealthProfile() {
        return healthProfile;
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
    }

    public void updateHealthProfile(Map<String, Object> healthProfile) {
        this.healthProfile = healthProfile;
    }

    public static User create(
            String email,
            String passwordHash,
            UserRole role,
            UserStatus status,
            Map<String, Object> healthProfile
    ) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.role = role == null ? UserRole.USER : role;
        user.status = status == null ? UserStatus.ACTIVE : status;
        user.healthProfile = healthProfile == null ? new HashMap<>() : new HashMap<>(healthProfile);
        return user;
    }
}
