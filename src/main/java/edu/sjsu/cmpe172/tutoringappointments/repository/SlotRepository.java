package edu.sjsu.cmpe172.tutoringappointments.repository;

import edu.sjsu.cmpe172.tutoringappointments.dto.SlotDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SlotRepository {

    private final JdbcTemplate jdbcTemplate;

    public SlotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SlotDto> findAllAvailable() {
        String sql = """
            SELECT
                a.id,
                u.name AS provider_name,
                s.name AS service_name,
                a.start_time,
                a.end_time,
                a.status
            FROM availability_slots a
            JOIN providers p ON a.provider_id = p.id
            JOIN users u ON p.user_id = u.id
            JOIN services s ON a.service_id = s.id
            WHERE a.status = 'AVAILABLE'
            ORDER BY a.start_time
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
            new SlotDto(
                rs.getLong("id"),
                rs.getString("provider_name"),
                rs.getString("service_name"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getString("status")
            )
        );
    }
}