package edu.sjsu.cmpe172.tutoringappointments;

import edu.sjsu.cmpe172.tutoringappointments.service.BookingException;
import edu.sjsu.cmpe172.tutoringappointments.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BookingIntegrationTests {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void exactlyOneOfTwoConcurrentBookingsSucceeds() throws Exception {
        long serviceId = jdbcTemplate.queryForObject(
            "SELECT id FROM services WHERE name = 'Calculus'", Long.class);
        long slotId = createSlot(serviceId);
        long[] studentIds = {
            findUser("john@sjsu.edu"),
            findUser("student2@sjsu.edu")
        };

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> attempts = new ArrayList<>();

            for (long studentId : studentIds) {
                attempts.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Start barrier timed out.");
                    }

                    try {
                        bookingService.book(studentId, slotId);
                        return 201;
                    } catch (BookingException exception) {
                        return exception.status();
                    }
                }));
            }

            boolean bothReady = ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            assertTrue(bothReady);

            List<Integer> outcomes = List.of(
                attempts.get(0).get(15, TimeUnit.SECONDS),
                attempts.get(1).get(15, TimeUnit.SECONDS)
            );

            assertEquals(1, outcomes.stream().filter(status -> status == 201).count());
            assertEquals(1, outcomes.stream().filter(status -> status == 409).count());
        }

        assertEquals(1, jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM appointments WHERE active_slot_id = ?", Integer.class, slotId));
        assertEquals("BOOKED", jdbcTemplate.queryForObject(
            "SELECT status FROM availability_slots WHERE id = ?", String.class, slotId));
    }

    private long findUser(String username) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = ?", Long.class, username);
    }

    private long createSlot(long serviceId) {
        long providerId = jdbcTemplate.queryForObject(
            "SELECT provider_id FROM services WHERE id = ?", Long.class, serviceId);
        LocalDateTime start = LocalDateTime.now().plusDays(60);
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                """
                INSERT INTO availability_slots (provider_id, service_id, start_time, end_time, status)
                VALUES (?, ?, ?, ?, 'AVAILABLE')
                """,
                new String[] { "id" }
            );
            statement.setLong(1, providerId);
            statement.setLong(2, serviceId);
            statement.setObject(3, start);
            statement.setObject(4, start.plusHours(1));
            return statement;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }
}
