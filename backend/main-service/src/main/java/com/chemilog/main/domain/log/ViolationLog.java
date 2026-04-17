package com.chemilog.main.domain.log;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "violation_logs")
public class ViolationLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_violation_logs_user"))
    private User user;

    @Column(name = "input_text", nullable = false, columnDefinition = "text")
    private String inputText;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_category", length = 50)
    private ViolationCategory violationCategory;

    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    protected ViolationLog() {
    }

    public Long getLogId() {
        return logId;
    }

    public User getUser() {
        return user;
    }

    public String getInputText() {
        return inputText;
    }

    public ViolationCategory getViolationCategory() {
        return violationCategory;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public static ViolationLog create(
            User user,
            String inputText,
            ViolationCategory category,
            BigDecimal confidenceScore
    ) {
        ViolationLog log = new ViolationLog();
        log.user = user;
        log.inputText = inputText;
        log.violationCategory = category;
        log.confidenceScore = confidenceScore;
        return log;
    }
}
