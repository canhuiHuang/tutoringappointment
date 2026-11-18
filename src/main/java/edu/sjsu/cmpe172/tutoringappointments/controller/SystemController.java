package edu.sjsu.cmpe172.tutoringappointments.controller;

import edu.sjsu.cmpe172.tutoringappointments.service.BookingNotifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SystemController {

    private static final Logger log = LoggerFactory.getLogger(SystemController.class);
    private final JdbcTemplate jdbcTemplate;
    private final BookingNotifications bookingNotifications;

    public SystemController(JdbcTemplate jdbcTemplate, BookingNotifications bookingNotifications) {
        this.jdbcTemplate = jdbcTemplate;
        this.bookingNotifications = bookingNotifications;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of("status", "UP", "database", "UP"));
        } catch (RuntimeException exception) {
            log.error("Database health check failed error={}", exception.getClass().getSimpleName());
            return ResponseEntity.status(503).body(Map.of("status", "DOWN", "database", "DOWN"));
        }
    }

    @GetMapping("/metrics")
    public Map<String, Long> metrics() {
        return bookingNotifications.metrics();
    }
}
