package com.chemilog.main.domain.food;

import com.chemilog.main.domain.common.SoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "food_additives_map",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_food_additives_map_food_additive",
                        columnNames = {"food_id", "additive_id"}
                )
        },
        indexes = {
                @Index(name = "idx_food_additives_map_food_id", columnList = "food_id"),
                @Index(name = "idx_food_additives_map_additive_id", columnList = "additive_id")
        }
)
public class FoodAdditiveMap extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "map_id")
    private Long mapId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false, foreignKey = @ForeignKey(name = "fk_food_additives_map_food"))
    private FoodItem food;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "additive_id", nullable = false, foreignKey = @ForeignKey(name = "fk_food_additives_map_additive"))
    private Additive additive;

    protected FoodAdditiveMap() {
    }

    public Long getMapId() {
        return mapId;
    }

    public FoodItem getFood() {
        return food;
    }

    public Additive getAdditive() {
        return additive;
    }

    public static FoodAdditiveMap create(FoodItem foodItem, Additive additive) {
        FoodAdditiveMap map = new FoodAdditiveMap();
        map.food = foodItem;
        map.additive = additive;
        return map;
    }
}
