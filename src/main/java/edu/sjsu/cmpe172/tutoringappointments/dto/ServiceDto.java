package edu.sjsu.cmpe172.tutoringappointments.dto;

public record ServiceDto(
    Long id,
    Long providerId,
    String name,
    String description,
    Integer durationMinutes,
    String providerName
) {}
