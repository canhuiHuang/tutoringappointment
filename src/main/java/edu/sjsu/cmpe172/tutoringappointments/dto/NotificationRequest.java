package edu.sjsu.cmpe172.tutoringappointments.dto;

public record NotificationRequest(
    String id,
    String recipient,
    String type,
    String message
) {}
