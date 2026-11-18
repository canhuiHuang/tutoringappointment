package edu.sjsu.cmpe172.tutoringappointments.dto;

public record ReminderResult(
    int scanned,
    int sent,
    int failed,
    int skipped
) {}
