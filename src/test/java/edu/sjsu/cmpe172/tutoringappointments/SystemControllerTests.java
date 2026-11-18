package edu.sjsu.cmpe172.tutoringappointments;

import edu.sjsu.cmpe172.tutoringappointments.controller.SystemController;
import edu.sjsu.cmpe172.tutoringappointments.service.BookingNotifications;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SystemControllerTests {

    @Test
    void databaseOutageReturnsServiceUnavailable() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
            .thenThrow(new DataAccessResourceFailureException("Database unavailable"));
        SystemController controller = new SystemController(jdbcTemplate, mock(BookingNotifications.class));

        var response = controller.health();

        assertEquals(503, response.getStatusCode().value());
        assertEquals(Map.of("status", "DOWN", "database", "DOWN"), response.getBody());
    }
}
