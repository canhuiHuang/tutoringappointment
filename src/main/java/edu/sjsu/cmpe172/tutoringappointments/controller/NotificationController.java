package edu.sjsu.cmpe172.tutoringappointments.controller;

import edu.sjsu.cmpe172.tutoringappointments.dto.NotificationRequest;
import edu.sjsu.cmpe172.tutoringappointments.repository.NotificationRepository;
import edu.sjsu.cmpe172.tutoringappointments.service.BookingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @PostMapping("/mock/notifications")
    public ResponseEntity<Void> accept(@RequestBody NotificationRequest notification) {
        if (notification.id() == null || notification.id().isBlank() || notification.id().length() > 100
                || notification.recipient() == null || notification.recipient().isBlank()
                || notification.recipient().length() > 100
                || notification.type() == null || notification.type().isBlank() || notification.type().length() > 40
                || notification.message() == null || notification.message().isBlank()
                || notification.message().length() > 1000) {
            throw new BookingException(400, "Supply a notification ID, recipient, type, and message within the allowed lengths.");
        }

        boolean created = notificationRepository.save(notification);
        log.info("Mock notification accepted id={} type={} duplicate={}",
            notification.id(), notification.type(), !created);
        return ResponseEntity.noContent().build();
    }
}
