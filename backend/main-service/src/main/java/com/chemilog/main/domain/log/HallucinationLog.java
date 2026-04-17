package com.chemilog.main.domain.log;

import com.chemilog.main.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "hallucination_logs")
public class HallucinationLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "prompt_context", columnDefinition = "text")
    private String promptContext;

    @Column(name = "generated_response", columnDefinition = "text")
    private String generatedResponse;

    @Column(name = "failed_reason", length = 255)
    private String failedReason;

    protected HallucinationLog() {
    }

    public Long getLogId() {
        return logId;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getPromptContext() {
        return promptContext;
    }

    public String getGeneratedResponse() {
        return generatedResponse;
    }

    public String getFailedReason() {
        return failedReason;
    }

    public static HallucinationLog create(
            String modelVersion,
            String promptContext,
            String generatedResponse,
            String failedReason
    ) {
        HallucinationLog log = new HallucinationLog();
        log.modelVersion = modelVersion;
        log.promptContext = promptContext;
        log.generatedResponse = generatedResponse;
        log.failedReason = failedReason;
        return log;
    }
}
