package edu.sjsu.cmpe172.tutoringappointments.dto;

import java.time.LocalDateTime;

public record SlotRequest(
    long serviceId,
    LocalDateTime startTime,
    LocalDateTime endTime
) {}
