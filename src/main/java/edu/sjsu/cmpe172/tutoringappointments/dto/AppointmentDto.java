package edu.sjsu.cmpe172.tutoringappointments.dto;

import java.time.LocalDateTime;

public record AppointmentDto(
    long id,
    String studentName,
    String providerName,
    String serviceName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String status
) {}
