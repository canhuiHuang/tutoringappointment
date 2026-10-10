package edu.sjsu.cmpe172.tutoringappointments.config;

import edu.sjsu.cmpe172.tutoringappointments.dto.AccountDto;
import edu.sjsu.cmpe172.tutoringappointments.service.BookingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AccessConfig implements WebMvcConfigurer {

    public static AccountDto account(HttpServletRequest request) {
        var session = request.getSession(false);
        AccountDto account = session == null ? null : (AccountDto) session.getAttribute("account");

        if (account == null) {
            throw new BookingException(401, "Please log in.");
        }

        return account;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String path = request.getRequestURI().substring(request.getContextPath().length());

                if (path.startsWith("/api/customer/") && !account(request).role().equals("STUDENT")) {
                    throw new BookingException(403, "Customer role required.");
                }

                if (path.startsWith("/api/provider/") && !account(request).role().equals("TUTOR")) {
                    throw new BookingException(403, "Provider role required.");
                }

                return true;
            }
        });
    }
}