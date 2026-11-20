package com.eiu.pdds.audit.event;

import java.util.UUID;

/**
 * Domain event published when an administrator or expert performs a privileged moderation or governance action.
 */
public record AdminAuditEvent(
        UUID actorUserId,
        String actorUsername,
        String action,
        String targetType,
        String targetId,
        Object beforeState,
        Object afterState
) {
}
