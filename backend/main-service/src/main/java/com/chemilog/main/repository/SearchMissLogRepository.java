package com.chemilog.main.repository;

import com.chemilog.main.domain.log.SearchMissLog;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchMissLogRepository extends JpaRepository<SearchMissLog, Long> {

    @Query("""
            SELECT s
            FROM SearchMissLog s
            WHERE s.deleted = false
              AND s.resolved = false
              AND LOWER(s.keyword) = LOWER(:keyword)
            """)
    Optional<SearchMissLog> findActiveByKeyword(@Param("keyword") String keyword);

    @Query("""
            SELECT s
            FROM SearchMissLog s
            WHERE s.deleted = false
              AND (:keyword = '' OR LOWER(s.keyword) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:resolved IS NULL OR s.resolved = :resolved)
            ORDER BY s.updatedAt DESC, s.hitCount DESC
            """)
    Page<SearchMissLog> searchForAdmin(
            @Param("keyword") String keyword,
            @Param("resolved") Boolean resolved,
            Pageable pageable
    );
}
