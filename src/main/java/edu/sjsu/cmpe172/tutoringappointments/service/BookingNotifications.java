package edu.sjsu.cmpe172.tutoringappointments.service;

import edu.sjsu.cmpe172.tutoringappointments.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class BookingNotifications {

    private static final Logger log = LoggerFactory.getLogger(BookingNotifications.class);
    private final NotificationGateway notificationGateway;
    private final AtomicLong bookings = new AtomicLong();
    private final AtomicLong cancellations = new AtomicLong();
    private final AtomicLong delivered = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    public BookingNotifications(NotificationGateway notificationGateway) {
        this.notificationGateway = notificationGateway;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(NotificationRequest notification) {
        if (notification.type().equals("BOOKED")) {
            bookings.incrementAndGet();
        } else if (notification.type().equals("CANCELLED")) {
            cancellations.incrementAndGet();
        }
        log.info("Appointment committed notificationId={} type={}", notification.id(), notification.type());
        if (notificationGateway.send(notification)) {
            delivered.incrementAndGet();
        } else {
            failed.incrementAndGet();
        }
    }

    public Map<String, Long> metrics() {
        return Map.of(
            "bookingsCommitted", bookings.get(),
            "cancellationsCommitted", cancellations.get(),
            "notificationsDelivered", delivered.get(),
            "notificationsFailed", failed.get()
        );
    }
}
