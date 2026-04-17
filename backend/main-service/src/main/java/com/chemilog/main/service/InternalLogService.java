package com.chemilog.main.service;

import com.chemilog.main.api.internal.HallucinationLogCreateRequest;
import com.chemilog.main.api.internal.ViolationLogCreateRequest;
import com.chemilog.main.domain.log.HallucinationLog;
import com.chemilog.main.domain.log.ViolationCategory;
import com.chemilog.main.domain.log.ViolationLog;
import com.chemilog.main.domain.user.User;
import com.chemilog.main.repository.HallucinationLogRepository;
import com.chemilog.main.repository.UserRepository;
import com.chemilog.main.repository.ViolationLogRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalLogService {

    private final ViolationLogRepository violationLogRepository;
    private final HallucinationLogRepository hallucinationLogRepository;
    private final UserRepository userRepository;

    public InternalLogService(
            ViolationLogRepository violationLogRepository,
            HallucinationLogRepository hallucinationLogRepository,
            UserRepository userRepository
    ) {
        this.violationLogRepository = violationLogRepository;
        this.hallucinationLogRepository = hallucinationLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createViolationLog(ViolationLogCreateRequest request) {
        User user = null;
        if (request.userId() != null) {
            user = userRepository.findById(request.userId()).orElse(null);
        }
        ViolationCategory category = toViolationCategory(request.violationCategory());
        violationLogRepository.save(ViolationLog.create(
                user,
                request.inputText(),
                category,
                request.confidenceScore()
        ));
    }

    @Transactional
    public void createHallucinationLog(HallucinationLogCreateRequest request) {
        hallucinationLogRepository.save(HallucinationLog.create(
                request.modelVersion(),
                request.promptContext(),
                request.generatedResponse(),
                request.failedReason()
        ));
    }

    private ViolationCategory toViolationCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ViolationCategory.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ViolationCategory.OUT_OF_DOMAIN;
        }
    }
}
