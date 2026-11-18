package edu.sjsu.cmpe172.tutoringappointments.dto;

import java.time.LocalDateTime;

public record PendingAction(
    String confirmationId,
    String action,
    long targetId,
    String summary,
    LocalDateTime expiresAt
) {}
