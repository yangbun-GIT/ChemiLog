package com.chemilog.main.service;

import com.chemilog.main.api.admin.AdminSearchMissRowResponse;
import com.chemilog.main.api.common.PageInfo;
import com.chemilog.main.api.common.PagedData;
import com.chemilog.main.domain.log.SearchMissLog;
import com.chemilog.main.exception.ApiException;
import com.chemilog.main.repository.SearchMissLogRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchMissLogService {

    private final SearchMissLogRepository searchMissLogRepository;

    public SearchMissLogService(SearchMissLogRepository searchMissLogRepository) {
        this.searchMissLogRepository = searchMissLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String keyword) {
        if (keyword == null) {
            return;
        }
        String normalized = keyword.trim();
        if (normalized.length() < 2) {
            return;
        }

        SearchMissLog log = searchMissLogRepository.findActiveByKeyword(normalized)
                .orElseGet(() -> SearchMissLog.create(normalized));
        if (log.getMissId() != null) {
            log.incrementHit();
        }
        searchMissLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PagedData<AdminSearchMissRowResponse> list(String keyword, Boolean resolved, int page, int size) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        Page<SearchMissLog> rows = searchMissLogRepository.searchForAdmin(
                normalizedKeyword,
                resolved,
                PageRequest.of(page, size)
        );
        return new PagedData<>(
                rows.getContent().stream()
                        .map(log -> new AdminSearchMissRowResponse(
                                log.getMissId(),
                                log.getKeyword(),
                                log.getHitCount(),
                                log.isResolved(),
                                log.getUpdatedAt()
                        ))
                        .toList(),
                new PageInfo(rows.getNumber(), rows.getTotalPages(), rows.getTotalElements(), rows.hasNext())
        );
    }

    @Transactional
    public void resolve(Long missId, boolean resolved) {
        SearchMissLog log = searchMissLogRepository.findById(missId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MISS-4040", "미등록 검색 로그를 찾을 수 없습니다."));

        if (log.isDeleted()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MISS-4041", "삭제된 미등록 검색 로그입니다.");
        }

        if (resolved) {
            log.markResolved();
        } else {
            log.markUnresolved();
        }
    }
}
