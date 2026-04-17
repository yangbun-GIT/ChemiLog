package com.chemilog.main.repository;

import com.chemilog.main.domain.meal.Meal;
import com.chemilog.main.domain.user.User;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealRepository extends JpaRepository<Meal, Long> {

    List<Meal> findByUserAndMealDate(User user, LocalDate mealDate);

    long countByMealDate(LocalDate mealDate);

    long countByMealDateBetween(LocalDate from, LocalDate to);

    List<Meal> findTop10ByOrderByCreatedAtDesc();
}
