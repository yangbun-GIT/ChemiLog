package com.chemilog.main.domain.log;

import com.chemilog.main.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "search_miss_logs")
public class SearchMissLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "miss_id")
    private Long missId;

    @Column(name = "keyword", nullable = false, length = 150)
    private String keyword;

    @Column(name = "hit_count", nullable = false)
    private Integer hitCount = 1;

    @Column(name = "is_resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    protected SearchMissLog() {
    }

    public Long getMissId() {
        return missId;
    }

    public String getKeyword() {
        return keyword;
    }

    public Integer getHitCount() {
        return hitCount;
    }

    public boolean isResolved() {
        return resolved;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public static SearchMissLog create(String keyword) {
        SearchMissLog log = new SearchMissLog();
        log.keyword = keyword;
        log.hitCount = 1;
        log.resolved = false;
        log.deleted = false;
        return log;
    }

    public void incrementHit() {
        this.hitCount = (this.hitCount == null ? 0 : this.hitCount) + 1;
    }

    public void markResolved() {
        this.resolved = true;
    }

    public void markUnresolved() {
        this.resolved = false;
    }
}
