package edu.sjsu.cmpe172.tutoringappointments;

import edu.sjsu.cmpe172.tutoringappointments.dto.NotificationRequest;
import edu.sjsu.cmpe172.tutoringappointments.service.NotificationGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationHttpTests {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void repeatedHttpNotificationIsStoredOnlyOnce() {
        NotificationGateway gateway = new NotificationGateway(
            "http://localhost:" + port + "/mock/notifications", 2000, 3, 1);
        NotificationRequest notification = new NotificationRequest(
            "idempotency-test", "john@sjsu.edu", "BOOKED", "Your appointment is confirmed.");

        assertTrue(gateway.send(notification));
        assertTrue(gateway.send(notification));
        assertEquals(1, jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE id = ?", Integer.class, notification.id()));
    }

    @Test
    void healthAndMetricsAreAvailableOverHttp() {
        RestClient client = RestClient.create("http://localhost:" + port);
        String health = client.get().uri("/health").retrieve().body(String.class);
        String metrics = client.get().uri("/metrics").retrieve().body(String.class);

        assertTrue(health.contains("\"database\":\"UP\""));
        assertTrue(metrics.contains("bookingsCommitted"));
    }
}
