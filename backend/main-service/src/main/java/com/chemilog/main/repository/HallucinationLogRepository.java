package com.chemilog.main.repository;

import com.chemilog.main.domain.log.HallucinationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HallucinationLogRepository extends JpaRepository<HallucinationLog, Long> {

    @Query("""
            SELECT h
            FROM HallucinationLog h
            WHERE (
                    :keyword = ''
                    OR LOWER(COALESCE(h.failedReason, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(h.generatedResponse, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY h.createdAt DESC
            """)
    Page<HallucinationLog> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);
}
