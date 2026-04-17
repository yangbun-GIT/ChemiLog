package com.chemilog.main.domain.log;

import com.chemilog.main.domain.common.BaseTimeEntity;
import com.chemilog.main.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false, foreignKey = @ForeignKey(name = "fk_admin_audit_logs_admin"))
    private User admin;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "target_entity", nullable = false, length = 50)
    private String targetEntity;

    @Column(name = "target_id")
    private Long targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> payloadSnapshot = new HashMap<>();

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    protected AdminAuditLog() {
    }

    public Long getAuditId() {
        return auditId;
    }

    public User getAdmin() {
        return admin;
    }

    public String getActionType() {
        return actionType;
    }

    public String getTargetEntity() {
        return targetEntity;
    }

    public Long getTargetId() {
        return targetId;
    }

    public Map<String, Object> getPayloadSnapshot() {
        return payloadSnapshot;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
