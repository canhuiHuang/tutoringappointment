package edu.sjsu.cmpe172.tutoringappointments.repository;

import edu.sjsu.cmpe172.tutoringappointments.dto.NotificationRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public NotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean save(NotificationRequest notification) {
        String sql = """
            INSERT INTO notifications (id, recipient, type, message)
            VALUES (?, ?, ?, ?)
            """;

        try {
            jdbcTemplate.update(sql, notification.id(), notification.recipient(),
                notification.type(), notification.message());
            return true;
        } catch (DuplicateKeyException exception) {
            // A retry of the same operation has already been accepted.
            return false;
        }
    }
}
