package edu.sjsu.cmpe172.tutoringappointments.dto;

public record LoginRequest(
    String username,
    String password
) {}
