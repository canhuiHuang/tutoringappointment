package edu.sjsu.cmpe172.tutoringappointments.controller;

import edu.sjsu.cmpe172.tutoringappointments.config.AccessConfig;
import edu.sjsu.cmpe172.tutoringappointments.dto.AppointmentDto;
import edu.sjsu.cmpe172.tutoringappointments.dto.BookRequest;
import edu.sjsu.cmpe172.tutoringappointments.dto.SlotRequest;
import edu.sjsu.cmpe172.tutoringappointments.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/customer/appointments")
    public ResponseEntity<?> book(HttpServletRequest request, @RequestBody BookRequest input) {
        long studentId = AccessConfig.account(request).id();
        long appointmentId = bookingService.book(studentId, input.slotId());

        return ResponseEntity.status(201).body(Map.of("appointmentId", appointmentId));
    }

    @GetMapping("/customer/appointments")
    public List<AppointmentDto> mine(HttpServletRequest request) {
        long studentId = AccessConfig.account(request).id();

        return bookingService.getAppointments(studentId, false);
    }

    @PostMapping("/customer/appointments/{id}/cancel")
    public ResponseEntity<?> cancel(HttpServletRequest request, @PathVariable long id) {
        long studentId = AccessConfig.account(request).id();
        bookingService.cancel(studentId, id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/provider/appointments")
    public List<AppointmentDto> provider(HttpServletRequest request) {
        long userId = AccessConfig.account(request).id();

        return bookingService.getAppointments(userId, true);
    }

    @PostMapping("/provider/slots")
    public ResponseEntity<?> create(HttpServletRequest request, @RequestBody SlotRequest input) {
        long userId = AccessConfig.account(request).id();
        bookingService.create(userId, input.serviceId(), input.startTime(), input.endTime());

        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/provider/slots/{id}")
    public ResponseEntity<?> remove(HttpServletRequest request, @PathVariable long id) {
        long userId = AccessConfig.account(request).id();
        bookingService.remove(userId, id);

        return ResponseEntity.noContent().build();
    }
}
