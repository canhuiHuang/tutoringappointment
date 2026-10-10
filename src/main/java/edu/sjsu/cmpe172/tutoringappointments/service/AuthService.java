package edu.sjsu.cmpe172.tutoringappointments.service;

import edu.sjsu.cmpe172.tutoringappointments.dto.AccountDto;
import edu.sjsu.cmpe172.tutoringappointments.repository.AccountRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AccountRepository accountRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountDto login(String username, String password) {
        if (username == null || username.isBlank() || username.length() > 100
                || password == null || password.isBlank() || password.length() > 72) {
            throw new BookingException(400, "Supply a username and password (at most 72 characters).");
        }

        var credentials = accountRepository.find(username.trim())
            .orElseThrow(() -> new BookingException(401, "Invalid username or password."));
        boolean validPassword = passwordEncoder.matches(password, credentials.hash());

        if (!validPassword) {
            throw new BookingException(401, "Invalid username or password.");
        }

        return credentials.account();
    }

    public Long getProviderId(long userId) {
        return accountRepository.findProviderId(userId).orElse(null);
    }
}
