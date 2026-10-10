package edu.sjsu.cmpe172.tutoringappointments.repository;

import edu.sjsu.cmpe172.tutoringappointments.dto.SlotDto;
import edu.sjsu.cmpe172.tutoringappointments.service.BookingException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SlotRepository {

    private final JdbcTemplate jdbcTemplate;

    public SlotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SlotDto> findAllAvailable() {
        return findAvailable(null, null, null, 0, 20);
    }

    public List<SlotDto> findAvailable(Long providerId, Long serviceId, LocalDate date, int page, int size) {
        return findAvailable(providerId, serviceId, date, page, size, null);
    }

    public List<SlotDto> findAvailable(Long providerId, Long serviceId, LocalDate date, int page, int size, LocalDate endDate) {
        if (endDate != null && (date == null || !endDate.isAfter(date))) {
            throw new BookingException(400, "End date must be later than the start date.");
        }
        if (page < 0 || page > 1000000 || size < 1 || size > 100
                || (providerId != null && providerId <= 0)
                || (serviceId != null && serviceId <= 0)) {
            throw new BookingException(400, "Invalid filter or pagination parameters.");
        }

        String sql = """
            SELECT
                a.id,
                a.service_id,
                u.name AS provider_name,
                s.name AS service_name,
                a.start_time,
                a.end_time,
                a.status
            FROM availability_slots a
            JOIN providers p ON a.provider_id = p.id
            JOIN users u ON p.user_id = u.id
            JOIN services s ON a.service_id = s.id
            WHERE a.status = 'AVAILABLE' AND a.start_time > CURRENT_TIMESTAMP
            """;

        List<Object> parameters = new ArrayList<>();

        if (providerId != null) {
            sql += " AND a.provider_id = ?";
            parameters.add(providerId);
        }

        if (serviceId != null) {
            sql += " AND a.service_id = ?";
            parameters.add(serviceId);
        }

        if (date != null) {
            sql += " AND a.start_time >= ? AND a.start_time < ?";
            parameters.add(date.atStartOfDay());
            parameters.add((endDate == null ? date.plusDays(1) : endDate).atStartOfDay());
        }

        sql += " ORDER BY a.start_time, a.id LIMIT ? OFFSET ?";
        parameters.add(size);
        parameters.add(page * size);

        return jdbcTemplate.query(sql, (rs, rowNum) ->
            new SlotDto(
                rs.getLong("id"),
                rs.getLong("service_id"),
                rs.getString("provider_name"),
                rs.getString("service_name"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getString("status")
            ),
            parameters.toArray()
        );
    }
}
