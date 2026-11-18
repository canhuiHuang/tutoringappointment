package edu.sjsu.cmpe172.tutoringappointments.repository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ReminderRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReminderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Upcoming(
        long appointmentId, String studentName, String email,
        String tutorName, String serviceName, LocalDateTime start
    ) {}

    public List<Upcoming> upcoming(Long providerUserId, LocalDateTime now, LocalDateTime until) {
        String sql = """
            SELECT a.id, student.name AS student_name, student.email,
                tutor.name AS tutor_name, s.name AS service_name, slot.start_time
            FROM appointments a
            JOIN users student ON student.id = a.student_id
            JOIN availability_slots slot ON slot.id = a.slot_id
            JOIN services s ON s.id = slot.service_id
            JOIN providers p ON p.id = slot.provider_id
            JOIN users tutor ON tutor.id = p.user_id
            LEFT JOIN ai_reminders r ON r.appointment_id = a.id
            WHERE a.status = 'BOOKED' AND slot.start_time > ? AND slot.start_time <= ?
                AND (r.appointment_id IS NULL OR (r.sent_at IS NULL AND r.claimed_at < ?))
            """;
        List<Object> parameters = new ArrayList<>(List.of(now, until, now.minusMinutes(10)));
        if (providerUserId != null) {
            sql += " AND p.user_id = ?";
            parameters.add(providerUserId);
        }
        sql += " ORDER BY slot.start_time, a.id LIMIT 20";
        return jdbcTemplate.query(sql, (rs, row) -> new Upcoming(
            rs.getLong("id"), rs.getString("student_name"), rs.getString("email"),
            rs.getString("tutor_name"), rs.getString("service_name"),
            rs.getTimestamp("start_time").toLocalDateTime()), parameters.toArray());
    }

    public boolean claim(long appointmentId) {
        LocalDateTime now = LocalDateTime.now();
        try {
            jdbcTemplate.update("INSERT INTO ai_reminders (appointment_id, claimed_at) VALUES (?, ?)", appointmentId, now);
            return true;
        } catch (DuplicateKeyException exception) {
            return jdbcTemplate.update("""
                UPDATE ai_reminders SET claimed_at = ?
                WHERE appointment_id = ? AND sent_at IS NULL AND claimed_at < ?
                """, now, appointmentId, now.minusMinutes(10)) == 1;
        }
    }

    public boolean stillUpcoming(long appointmentId) {
        return jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM appointments a JOIN availability_slots s ON s.id = a.slot_id
            WHERE a.id = ? AND a.status = 'BOOKED' AND s.start_time > CURRENT_TIMESTAMP
            """, Integer.class, appointmentId) == 1;
    }

    public String message(long appointmentId) {
        return jdbcTemplate.queryForObject("SELECT message FROM ai_reminders WHERE appointment_id = ?",
            String.class, appointmentId);
    }

    public void saveMessage(long appointmentId, String message) {
        jdbcTemplate.update("UPDATE ai_reminders SET message = ? WHERE appointment_id = ?", message, appointmentId);
    }

    public void markSent(long appointmentId) {
        jdbcTemplate.update("UPDATE ai_reminders SET sent_at = CURRENT_TIMESTAMP WHERE appointment_id = ?", appointmentId);
    }
}
