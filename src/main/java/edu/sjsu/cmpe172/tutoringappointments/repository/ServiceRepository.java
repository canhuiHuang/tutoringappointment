package edu.sjsu.cmpe172.tutoringappointments.repository;

import edu.sjsu.cmpe172.tutoringappointments.dto.ServiceDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ServiceRepository {

    private final JdbcTemplate jdbcTemplate;

    public ServiceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ServiceDto> findAll() {
        String sql = """
            SELECT
                s.id,
                s.provider_id,
                s.name,
                s.description,
                s.duration,
                u.name AS provider_name
            FROM services s
            JOIN providers p ON s.provider_id = p.id
            JOIN users u ON p.user_id = u.id
            ORDER BY s.name
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
            new ServiceDto(
                rs.getLong("id"),
                rs.getLong("provider_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("duration"),
                rs.getString("provider_name")
            )
        );
    }
}
