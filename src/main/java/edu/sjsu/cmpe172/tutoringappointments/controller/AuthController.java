package edu.sjsu.cmpe172.tutoringappointments.controller;

import edu.sjsu.cmpe172.tutoringappointments.dto.AccountDto;
import edu.sjsu.cmpe172.tutoringappointments.dto.LoginRequest;
import edu.sjsu.cmpe172.tutoringappointments.dto.SessionDto;
import edu.sjsu.cmpe172.tutoringappointments.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/session")
    public SessionDto session(HttpServletRequest request) {
        var session = request.getSession(false);
        AccountDto user = session == null ? null : (AccountDto) session.getAttribute("account");
        Long providerId = null;

        if (user != null && user.role().equals("TUTOR")) {
            providerId = authService.getProviderId(user.id());
        }

        return new SessionDto(
            user,
            providerId
        );
    }

    @PostMapping("/login")
    public SessionDto login(HttpServletRequest request, @RequestBody LoginRequest input) {
        AccountDto user = authService.login(input.username(), input.password());

        var session = request.getSession();
        session.setAttribute("account", user);

        return session(request);
    }

    @PostMapping("/logout")
    public SessionDto logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return session(request);
    }
}
