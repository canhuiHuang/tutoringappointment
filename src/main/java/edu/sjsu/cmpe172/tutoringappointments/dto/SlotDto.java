package edu.sjsu.cmpe172.tutoringappointments.dto;

import java.time.LocalDateTime;

public record SlotDto(
    Long id,
    Long serviceId,
    String providerName,
    String serviceName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String status
) {}
