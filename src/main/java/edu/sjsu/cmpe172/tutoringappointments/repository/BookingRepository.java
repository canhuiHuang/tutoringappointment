package edu.sjsu.cmpe172.tutoringappointments.repository;

import edu.sjsu.cmpe172.tutoringappointments.dto.AppointmentDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class BookingRepository {

    private final JdbcTemplate jdbcTemplate;

    public BookingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record LockedSlot(
        long id,
        long providerId,
        long serviceId,
        LocalDateTime start,
        LocalDateTime end,
        String status,
        int duration
    ) {}

    public record Booking(
        long id,
        long studentId,
        long slotId,
        String status
    ) {}

    public Optional<LockedSlot> lockSlot(long id) {
        String sql = """
            SELECT
                a.*,
                s.duration
            FROM availability_slots a
            JOIN services s ON a.service_id = s.id
            WHERE a.id = ?
            FOR UPDATE OF a
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
            new LockedSlot(
                rs.getLong("id"),
                rs.getLong("provider_id"),
                rs.getLong("service_id"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getString("status"),
                rs.getInt("duration")
            ),
            id
        ).stream().findFirst();
    }

    public long insertBooking(long studentId, long slotId) {
        String sql = """
            INSERT INTO appointments (student_id, slot_id, active_slot_id, status)
            VALUES (?, ?, ?, 'BOOKED')
            """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[] { "id" });
            statement.setLong(1, studentId);
            statement.setLong(2, slotId);
            statement.setLong(3, slotId);
            return statement;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public void slotStatus(long id, String status) {
        String sql = """
            UPDATE availability_slots
            SET status = ?
            WHERE id = ?
            """;

        jdbcTemplate.update(sql, status, id);
    }

    public Optional<Booking> booking(long id) {
        String sql = """
            SELECT id, student_id, slot_id, status
            FROM appointments
            WHERE id = ?
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
            new Booking(
                rs.getLong("id"),
                rs.getLong("student_id"),
                rs.getLong("slot_id"),
                rs.getString("status")
            ),
            id
        ).stream().findFirst();
    }

    public void cancel(long id) {
        String sql = """
            UPDATE appointments
            SET status = 'CANCELLED', active_slot_id = NULL
            WHERE id = ?
            """;

        jdbcTemplate.update(sql, id);
    }

    public Optional<Long> provider(long userId) {
        String sql = """
            SELECT id
            FROM providers
            WHERE user_id = ?
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
            rs.getLong("id"),
            userId
        ).stream().findFirst();
    }

    public Optional<Integer> serviceDuration(long serviceId, long providerId) {
        String sql = """
            SELECT duration
            FROM services
            WHERE id = ? AND provider_id = ?
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
            rs.getInt("duration"),
            serviceId,
            providerId
        ).stream().findFirst();
    }

    public void createSlot(long providerId, long serviceId, LocalDateTime start, LocalDateTime end) {
        String sql = """
            INSERT INTO availability_slots (provider_id, service_id, start_time, end_time, status)
            VALUES (?, ?, ?, ?, 'AVAILABLE')
            """;

        jdbcTemplate.update(sql, providerId, serviceId, start, end);
    }

    @Transactional
    public List<AppointmentDto> appointments(long userId, boolean provider) {
        String completeSql = """
            UPDATE appointments
            SET status = 'COMPLETED'
            WHERE status = 'BOOKED' AND slot_id IN (
                SELECT id
                FROM availability_slots
                WHERE end_time <= CURRENT_TIMESTAMP
            )
            """;

        jdbcTemplate.update(completeSql);

        String sql = """
            SELECT
                a.id,
                student.name AS student_name,
                tutor.name AS provider_name,
                s.name AS service_name,
                slot.start_time,
                slot.end_time,
                a.status
            FROM appointments a
            JOIN users student ON student.id = a.student_id
            JOIN availability_slots slot ON slot.id = a.slot_id
            JOIN services s ON s.id = slot.service_id
            JOIN providers p ON p.id = slot.provider_id
            JOIN users tutor ON tutor.id = p.user_id
            """;

        if (provider) {
            sql += " WHERE p.user_id = ?";
        } else {
            sql += " WHERE a.student_id = ?";
        }

        sql += " ORDER BY slot.start_time DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
            new AppointmentDto(
                rs.getLong("id"),
                rs.getString("student_name"),
                rs.getString("provider_name"),
                rs.getString("service_name"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(),
                "BOOKED".equals(rs.getString("status"))
                    && rs.getTimestamp("end_time").toLocalDateTime().isBefore(LocalDateTime.now())
                    ? "COMPLETED" : rs.getString("status")
            ),
            userId
        );
    }
}
