package com.chemilog.main.repository;

import com.chemilog.main.domain.user.User;
import com.chemilog.main.domain.user.UserRole;
import com.chemilog.main.domain.user.UserStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedFalse(String email);

    Optional<User> findByUserIdAndDeletedFalse(Long userId);

    boolean existsByEmailAndDeletedFalse(String email);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.deleted = false
              AND (:keyword = '' OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR u.status = :status)
              AND (:role IS NULL OR u.role = :role)
            ORDER BY u.createdAt DESC
            """)
    Page<User> searchForAdmin(
            @Param("keyword") String keyword,
            @Param("status") UserStatus status,
            @Param("role") UserRole role,
            Pageable pageable
    );

    long countByDeletedFalse();

    long countByRoleAndDeletedFalse(UserRole role);

    long countByStatusAndDeletedFalse(UserStatus status);
}
