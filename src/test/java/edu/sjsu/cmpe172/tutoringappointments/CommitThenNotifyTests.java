package edu.sjsu.cmpe172.tutoringappointments;

import edu.sjsu.cmpe172.tutoringappointments.dto.NotificationRequest;
import edu.sjsu.cmpe172.tutoringappointments.service.BookingService;
import edu.sjsu.cmpe172.tutoringappointments.service.NotificationGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class CommitThenNotifyTests {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private NotificationGateway notificationGateway;

    @Test
    void changesAreVisibleToAnotherConnectionBeforeNotification() throws Exception {
        long studentId = jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = 'john@sjsu.edu'", Long.class);
        long serviceId = jdbcTemplate.queryForObject(
            "SELECT id FROM services WHERE name = 'Calculus'", Long.class);
        LocalDateTime start = LocalDateTime.now().plusDays(90).withNano(0);
        jdbcTemplate.update("""
            INSERT INTO availability_slots (provider_id, service_id, start_time, end_time, status)
            SELECT provider_id, id, ?, ?, 'AVAILABLE' FROM services WHERE id = ?
            """, start, start.plusHours(1), serviceId);
        long slotId = jdbcTemplate.queryForObject(
            "SELECT id FROM availability_slots WHERE service_id = ? AND start_time = ?",
            Long.class, serviceId, start);
        AtomicInteger checked = new AtomicInteger();

        when(notificationGateway.send(any())).thenAnswer(invocation -> {
            NotificationRequest notification = invocation.getArgument(0);
            // Bypass Spring's thread-bound JDBC connection: another transaction
            // must already be able to read the committed appointment status.
            try (var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(
                     "SELECT status FROM appointments WHERE slot_id = ?")) {
                statement.setLong(1, slotId);
                try (var result = statement.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(notification.type(), result.getString("status"));
                }
            }
            assertEquals("john@sjsu.edu", notification.recipient());
            checked.incrementAndGet();
            return true;
        });

        long appointmentId = bookingService.book(studentId, slotId);
        bookingService.cancel(studentId, appointmentId);

        assertEquals(2, checked.get());
    }
}
