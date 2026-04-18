package com.chemilog.main.repository;

import com.chemilog.main.domain.food.FoodAdditiveMap;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FoodAdditiveMapRepository extends JpaRepository<FoodAdditiveMap, Long> {

    List<FoodAdditiveMap> findByFoodFoodIdAndDeletedFalse(Long foodId);

    List<FoodAdditiveMap> findByFoodFoodId(Long foodId);

    List<FoodAdditiveMap> findByFoodFoodIdInAndDeletedFalse(List<Long> foodIds);

    @Query("""
            SELECT fam.additive.additiveId, COUNT(fam)
            FROM FoodAdditiveMap fam
            WHERE fam.deleted = false
              AND fam.food.deleted = false
            GROUP BY fam.additive.additiveId
            """)
    List<Object[]> countByAdditive();
}
