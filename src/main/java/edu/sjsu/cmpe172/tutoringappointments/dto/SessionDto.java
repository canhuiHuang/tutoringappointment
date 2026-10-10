package edu.sjsu.cmpe172.tutoringappointments.dto;

public record SessionDto(
    AccountDto user,
    Long providerId
) {}
