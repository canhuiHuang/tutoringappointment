package edu.sjsu.cmpe172.tutoringappointments.repository;

import edu.sjsu.cmpe172.tutoringappointments.dto.AccountDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Credentials(
        AccountDto account,
        String hash
    ) {}

    public Optional<Credentials> find(String username) {
        // The existing email column accepts either a username or an email login.
        String sql = """
            SELECT
                id,
                name,
                role,
                password_hash
            FROM users
            WHERE email = ?
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
            new Credentials(
                new AccountDto(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("role")
                ),
                rs.getString("password_hash")
            ),
            username
        ).stream().findFirst();
    }

    public Optional<Long> findProviderId(long userId) {
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
}
