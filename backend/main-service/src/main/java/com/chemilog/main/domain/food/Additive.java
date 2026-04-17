package com.chemilog.main.domain.food;

import com.chemilog.main.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "additives")
public class Additive extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "additive_id")
    private Long additiveId;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "purpose", length = 50)
    private String purpose;

    @Column(name = "danger_level", nullable = false)
    private Integer dangerLevel;

    @Column(name = "daily_acceptable_intake", length = 100)
    private String dailyAcceptableIntake;

    protected Additive() {
    }

    public Long getAdditiveId() {
        return additiveId;
    }

    public String getName() {
        return name;
    }

    public String getPurpose() {
        return purpose;
    }

    public Integer getDangerLevel() {
        return dangerLevel;
    }

    public String getDailyAcceptableIntake() {
        return dailyAcceptableIntake;
    }
}
