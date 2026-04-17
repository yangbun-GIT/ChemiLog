package com.chemilog.main.domain.food;

import com.chemilog.main.domain.common.SoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "food_items")
public class FoodItem extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "food_id")
    private Long foodId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "category", nullable = false, length = 50)
    private String category = "기타";

    @Column(name = "barcode", unique = true, length = 50)
    private String barcode;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "calories", nullable = false, precision = 6, scale = 2)
    private BigDecimal calories;

    @Column(name = "carbs", nullable = false, precision = 6, scale = 2)
    private BigDecimal carbs = BigDecimal.ZERO;

    @Column(name = "protein", nullable = false, precision = 6, scale = 2)
    private BigDecimal protein = BigDecimal.ZERO;

    @Column(name = "fat", nullable = false, precision = 6, scale = 2)
    private BigDecimal fat = BigDecimal.ZERO;

    @Column(name = "sugars", nullable = false, precision = 6, scale = 2)
    private BigDecimal sugars = BigDecimal.ZERO;

    @Column(name = "sodium", nullable = false, precision = 6, scale = 2)
    private BigDecimal sodium = BigDecimal.ZERO;

    protected FoodItem() {
    }

    public Long getFoodId() {
        return foodId;
    }

    public String getName() {
        return name;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getCategory() {
        return category;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getCalories() {
        return calories;
    }

    public BigDecimal getCarbs() {
        return carbs;
    }

    public BigDecimal getProtein() {
        return protein;
    }

    public BigDecimal getFat() {
        return fat;
    }

    public BigDecimal getSugars() {
        return sugars;
    }

    public BigDecimal getSodium() {
        return sodium;
    }

    public static FoodItem create(
            String name,
            String category,
            String manufacturer,
            String barcode,
            String imageUrl,
            BigDecimal calories,
            BigDecimal carbs,
            BigDecimal protein,
            BigDecimal fat,
            BigDecimal sugars,
            BigDecimal sodium
    ) {
        FoodItem foodItem = new FoodItem();
        foodItem.name = name;
        foodItem.category = category == null || category.isBlank() ? "기타" : category.trim();
        foodItem.manufacturer = manufacturer;
        foodItem.barcode = barcode;
        foodItem.imageUrl = imageUrl;
        foodItem.calories = calories;
        foodItem.carbs = carbs == null ? BigDecimal.ZERO : carbs;
        foodItem.protein = protein == null ? BigDecimal.ZERO : protein;
        foodItem.fat = fat == null ? BigDecimal.ZERO : fat;
        foodItem.sugars = sugars == null ? BigDecimal.ZERO : sugars;
        foodItem.sodium = sodium == null ? BigDecimal.ZERO : sodium;
        return foodItem;
    }

    public void update(
            String name,
            String category,
            String manufacturer,
            String barcode,
            String imageUrl,
            BigDecimal calories,
            BigDecimal carbs,
            BigDecimal protein,
            BigDecimal fat,
            BigDecimal sugars,
            BigDecimal sodium
    ) {
        this.name = name;
        this.category = category == null || category.isBlank() ? "기타" : category.trim();
        this.manufacturer = manufacturer;
        this.barcode = barcode;
        this.imageUrl = imageUrl;
        this.calories = calories;
        this.carbs = carbs == null ? BigDecimal.ZERO : carbs;
        this.protein = protein == null ? BigDecimal.ZERO : protein;
        this.fat = fat == null ? BigDecimal.ZERO : fat;
        this.sugars = sugars == null ? BigDecimal.ZERO : sugars;
        this.sodium = sodium == null ? BigDecimal.ZERO : sodium;
    }
}
