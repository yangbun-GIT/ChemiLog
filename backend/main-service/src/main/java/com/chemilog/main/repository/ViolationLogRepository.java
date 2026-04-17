package com.chemilog.main.repository;

import com.chemilog.main.domain.log.ViolationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViolationLogRepository extends JpaRepository<ViolationLog, Long> {
}
