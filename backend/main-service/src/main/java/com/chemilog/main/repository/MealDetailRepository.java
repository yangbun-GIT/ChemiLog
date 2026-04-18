package com.chemilog.main.repository;

import com.chemilog.main.domain.meal.MealDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealDetailRepository extends JpaRepository<MealDetail, Long> {

    List<MealDetail> findByMealMealIdIn(List<Long> mealIds);

    List<MealDetail> findByMealMealId(Long mealId);
}
