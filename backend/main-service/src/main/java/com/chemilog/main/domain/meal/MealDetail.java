package com.chemilog.main.domain.meal;

import com.chemilog.main.domain.common.BaseTimeEntity;
import com.chemilog.main.domain.food.FoodItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "meal_details")
public class MealDetail extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_id", nullable = false, foreignKey = @ForeignKey(name = "fk_meal_details_meal"))
    private Meal meal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false, foreignKey = @ForeignKey(name = "fk_meal_details_food"))
    private FoodItem food;

    @Column(name = "quantity", nullable = false, precision = 5, scale = 2)
    private BigDecimal quantity;

    protected MealDetail() {
    }

    public Long getDetailId() {
        return detailId;
    }

    public Meal getMeal() {
        return meal;
    }

    public FoodItem getFood() {
        return food;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public static MealDetail create(Meal meal, FoodItem food, BigDecimal quantity) {
        MealDetail detail = new MealDetail();
        detail.meal = meal;
        detail.food = food;
        detail.quantity = quantity;
        return detail;
    }

    public void updateQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
