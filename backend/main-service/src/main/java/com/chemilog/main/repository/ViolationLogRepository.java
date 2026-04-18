package com.chemilog.main.repository;

import com.chemilog.main.domain.log.ViolationLog;
import com.chemilog.main.domain.log.ViolationCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ViolationLogRepository extends JpaRepository<ViolationLog, Long> {

    @Query("""
            SELECT v
            FROM ViolationLog v
            WHERE (:category IS NULL OR v.violationCategory = :category)
              AND (
                    :keyword = ''
                    OR LOWER(COALESCE(v.inputText, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY v.createdAt DESC
            """)
    Page<ViolationLog> searchForAdmin(
            @Param("category") ViolationCategory category,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
