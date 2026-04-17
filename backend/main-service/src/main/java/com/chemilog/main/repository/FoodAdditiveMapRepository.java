package com.chemilog.main.repository;

import com.chemilog.main.domain.food.FoodAdditiveMap;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodAdditiveMapRepository extends JpaRepository<FoodAdditiveMap, Long> {

    List<FoodAdditiveMap> findByFoodFoodIdAndDeletedFalse(Long foodId);

    List<FoodAdditiveMap> findByFoodFoodId(Long foodId);
}
