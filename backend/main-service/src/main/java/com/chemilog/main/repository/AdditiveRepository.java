package com.chemilog.main.repository;

import com.chemilog.main.domain.food.Additive;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdditiveRepository extends JpaRepository<Additive, Long> {

    List<Additive> findAllByAdditiveIdIn(Collection<Long> additiveIds);

    @Query("""
            SELECT a
            FROM Additive a
            WHERE :keyword = ''
               OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(a.purpose, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Additive> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);

    Optional<Additive> findByNameIgnoreCase(String name);

    Optional<Additive> findByAdditiveId(Long additiveId);
}
