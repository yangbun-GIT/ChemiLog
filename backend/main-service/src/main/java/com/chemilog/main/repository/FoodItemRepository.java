package com.chemilog.main.repository;

import com.chemilog.main.domain.food.FoodItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    @Query("""
            SELECT f
            FROM FoodItem f
            WHERE f.deleted = false
              AND (:category = '' OR f.category = :category)
              AND (
                    :keyword = ''
                    OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(f.manufacturer, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<FoodItem> search(@Param("keyword") String keyword, @Param("category") String category, Pageable pageable);

    long countByDeletedFalse();

    List<FoodItem> findTop8ByDeletedFalseOrderByCreatedAtDesc();

    @Query("""
            SELECT f.category, COUNT(f)
            FROM FoodItem f
            WHERE f.deleted = false
            GROUP BY f.category
            ORDER BY COUNT(f) DESC
            """)
    List<Object[]> countByCategory();

    @Query("""
            SELECT f
            FROM FoodItem f
            LEFT JOIN MealDetail md ON md.food = f
            WHERE f.deleted = false
            GROUP BY f
            ORDER BY COALESCE(SUM(md.quantity), 0) DESC, f.createdAt DESC
            """)
    List<FoodItem> findPopularFoods(Pageable pageable);

    Optional<FoodItem> findByFoodIdAndDeletedFalse(Long foodId);
}
