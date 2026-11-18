package edu.sjsu.cmpe172.tutoringappointments;

import edu.sjsu.cmpe172.tutoringappointments.repository.ReminderRepository;
import edu.sjsu.cmpe172.tutoringappointments.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReminderRepositoryTests {

    @Autowired private ReminderRepository reminders;
    @Autowired private BookingService bookingService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void reminderClaimsAreDurableScopedAndRecoverAfterLeaseExpiry() {
        long studentId = user("john@sjsu.edu");
        long aliceId = user("alice@sjsu.edu");
        long bobId = user("bob@sjsu.edu");
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        var keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO availability_slots (provider_id, service_id, start_time, end_time, status)
                SELECT provider_id, id, ?, ?, 'AVAILABLE' FROM services WHERE name = 'Algebra'
                """, new String[] {"id"});
            statement.setObject(1, start); statement.setObject(2, start.plusHours(1));
            return statement;
        }, keys);
        long appointmentId = bookingService.book(studentId, keys.getKey().longValue());
        LocalDateTime now = LocalDateTime.now();

        assertTrue(reminders.upcoming(aliceId, now, now.plusHours(24)).stream()
            .anyMatch(item -> item.appointmentId() == appointmentId));
        assertTrue(reminders.upcoming(bobId, now, now.plusHours(24)).stream()
            .noneMatch(item -> item.appointmentId() == appointmentId));
        assertTrue(reminders.claim(appointmentId));
        assertFalse(reminders.claim(appointmentId));
        reminders.saveMessage(appointmentId, "Saved draft");
        jdbcTemplate.update("UPDATE ai_reminders SET claimed_at = ? WHERE appointment_id = ?",
            now.minusMinutes(11), appointmentId);
        assertTrue(reminders.claim(appointmentId));
        assertEquals("Saved draft", reminders.message(appointmentId));
        reminders.markSent(appointmentId);
        assertFalse(reminders.claim(appointmentId));
        assertTrue(reminders.upcoming(aliceId, now, now.plusHours(24)).stream()
            .noneMatch(item -> item.appointmentId() == appointmentId));
    }

    private long user(String email) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }
}
