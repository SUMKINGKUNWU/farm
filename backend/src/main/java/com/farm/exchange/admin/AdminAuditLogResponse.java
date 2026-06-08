package com.farm.exchange.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AdminAuditLogResponse {

    private final UUID auditId;
    private final UUID adminUserId;
    private final String adminUsername;
    private final String action;
    private final String targetType;
    private final UUID targetId;
    private final String reason;
    private final OffsetDateTime createdAt;

    public AdminAuditLogResponse(UUID auditId, UUID adminUserId, String adminUsername, String action, String targetType, UUID targetId, String reason, OffsetDateTime createdAt) {
        this.auditId = auditId;
        this.adminUserId = adminUserId;
        this.adminUsername = adminUsername;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public UUID getAuditId() {
        return auditId;
    }

    public UUID getAdminUserId() {
        return adminUserId;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getReason() {
        return reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
