package com.chemilog.main.repository;

import com.chemilog.main.domain.log.HallucinationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HallucinationLogRepository extends JpaRepository<HallucinationLog, Long> {
}
