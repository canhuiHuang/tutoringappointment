package edu.sjsu.cmpe172.tutoringappointments.service;

import edu.sjsu.cmpe172.tutoringappointments.dto.AppointmentDto;
import edu.sjsu.cmpe172.tutoringappointments.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long book(long studentId, long slotId) {
        if (slotId <= 0) {
            throw new BookingException(400, "Slot ID must be positive.");
        }

        var slot = bookingRepository.lockSlot(slotId)
            .orElseThrow(() -> new BookingException(404, "Slot not found."));

        if (!slot.status().equals("AVAILABLE")) {
            throw new BookingException(409, "This slot is no longer available.");
        }

        if (!slot.start().isAfter(LocalDateTime.now())) {
            throw new BookingException(409, "This slot has already started.");
        }

        long appointmentId = bookingRepository.insertBooking(studentId, slotId);
        bookingRepository.slotStatus(slotId, "BOOKED");

        return appointmentId;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancel(long studentId, long appointmentId) {
        var initialBooking = bookingRepository.booking(appointmentId)
            .orElseThrow(() -> new BookingException(404, "Appointment not found."));

        if (initialBooking.studentId() != studentId) {
            throw new BookingException(403, "Forbidden.");
        }

        var slot = bookingRepository.lockSlot(initialBooking.slotId())
            .orElseThrow(() -> new BookingException(404, "Slot not found."));
        var booking = bookingRepository.booking(appointmentId).orElseThrow();

        if (booking.status().equals("CANCELLED")) {
            return;
        }

        if (!slot.start().isAfter(LocalDateTime.now())) {
            throw new BookingException(409, "Started appointments cannot be cancelled.");
        }

        bookingRepository.cancel(appointmentId);
        bookingRepository.slotStatus(slot.id(), "AVAILABLE");
    }

    @Transactional
    public void create(long userId, long serviceId, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isAfter(LocalDateTime.now()) || !end.isAfter(start)) {
            throw new BookingException(400, "Supply a future start time and a later end time.");
        }

        long providerId = bookingRepository.provider(userId)
            .orElseThrow(() -> new BookingException(403, "Provider profile required."));
        int duration = bookingRepository.serviceDuration(serviceId, providerId)
            .orElseThrow(() -> new BookingException(403, "Choose one of your own services."));

        if (Duration.between(start, end).getSeconds() != duration * 60L) {
            throw new BookingException(400, "Slot length must match the service duration.");
        }

        bookingRepository.createSlot(providerId, serviceId, start, end);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void remove(long userId, long slotId) {
        long providerId = bookingRepository.provider(userId)
            .orElseThrow(() -> new BookingException(403, "Provider profile required."));
        var slot = bookingRepository.lockSlot(slotId)
            .orElseThrow(() -> new BookingException(404, "Slot not found."));

        if (slot.providerId() != providerId) {
            throw new BookingException(403, "This is another provider's slot.");
        }

        if (!slot.status().equals("AVAILABLE")) {
            throw new BookingException(409, "Only available slots can be removed.");
        }

        // Preserve foreign keys and cancelled appointment history.
        bookingRepository.slotStatus(slotId, "REMOVED");
    }

    public List<AppointmentDto> getAppointments(long userId, boolean provider) {
        return bookingRepository.appointments(userId, provider);
    }
}
