package com.sourabh.user_service.entity;

public enum UserStatus {

    PENDING_VERIFICATION,
    PENDING_DETAILS,      // seller verified email but hasn't submitted business/ID details yet
    PENDING_APPROVAL,     // seller submitted details, awaiting admin approval
    ACTIVE,
    BLOCKED,
    DELETED
}
