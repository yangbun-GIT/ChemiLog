package com.chemilog.main.domain.meal;

import com.chemilog.main.domain.common.BaseTimeEntity;
import com.chemilog.main.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "meals",
        indexes = {
                @Index(name = "idx_meals_user_id", columnList = "user_id"),
                @Index(name = "idx_meals_date", columnList = "meal_date")
        }
)
public class Meal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meal_id")
    private Long mealId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_meals_user"))
    private User user;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @Column(name = "total_calories", precision = 8, scale = 2)
    private BigDecimal totalCalories;

    @Column(name = "health_score")
    private Integer healthScore;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    protected Meal() {
    }

    public Long getMealId() {
        return mealId;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getMealDate() {
        return mealDate;
    }

    public MealType getMealType() {
        return mealType;
    }

    public BigDecimal getTotalCalories() {
        return totalCalories;
    }

    public Integer getHealthScore() {
        return healthScore;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public static Meal create(
            User user,
            LocalDate mealDate,
            MealType mealType,
            BigDecimal totalCalories
    ) {
        Meal meal = new Meal();
        meal.user = user;
        meal.mealDate = mealDate;
        meal.mealType = mealType;
        meal.totalCalories = totalCalories;
        return meal;
    }

    public void updateHealthScore(Integer healthScore) {
        this.healthScore = healthScore;
    }

    public void updateTotalCalories(BigDecimal totalCalories) {
        this.totalCalories = totalCalories;
    }

    public void updateMealInfo(LocalDate mealDate, MealType mealType) {
        this.mealDate = mealDate;
        this.mealType = mealType;
    }

    public void markDeleted() {
        this.deleted = true;
    }

    public void restore() {
        this.deleted = false;
    }
}
