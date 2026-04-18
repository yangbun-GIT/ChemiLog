package com.chemilog.main.repository;

import com.chemilog.main.domain.meal.Meal;
import com.chemilog.main.domain.user.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealRepository extends JpaRepository<Meal, Long> {

    List<Meal> findByUserAndMealDateAndDeletedFalse(User user, LocalDate mealDate);

    long countByMealDateAndDeletedFalse(LocalDate mealDate);

    long countByMealDateBetweenAndDeletedFalse(LocalDate from, LocalDate to);

    List<Meal> findTop10ByDeletedFalseOrderByCreatedAtDesc();

    Optional<Meal> findByMealIdAndUserAndDeletedFalse(Long mealId, User user);
}
