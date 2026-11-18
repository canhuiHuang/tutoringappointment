package edu.sjsu.cmpe172.tutoringappointments.service;

import edu.sjsu.cmpe172.tutoringappointments.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificationGateway.class);
    private final RestClient restClient;
    private final String url;
    private final int attempts;
    private final long retryDelayMs;

    public NotificationGateway(
            @Value("${notification.url}") String url,
            @Value("${notification.timeout-ms:500}") int timeoutMs,
            @Value("${notification.attempts:3}") int attempts,
            @Value("${notification.retry-delay-ms:100}") long retryDelayMs) {
        if (timeoutMs < 1 || attempts < 1 || attempts > 5 || retryDelayMs < 0) {
            throw new IllegalArgumentException("Invalid notification timeout or retry settings.");
        }
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.url = url;
        this.attempts = attempts;
        this.retryDelayMs = retryDelayMs;
    }

    public boolean send(NotificationRequest notification) {
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                restClient.post().uri(url).contentType(MediaType.APPLICATION_JSON)
                    .body(notification).retrieve().toBodilessEntity();
                log.info("Notification delivered id={} attempt={}", notification.id(), attempt);
                return true;
            } catch (RuntimeException exception) {
                log.warn("Notification attempt failed id={} attempt={} error={}",
                    notification.id(), attempt, exception.getClass().getSimpleName());
                boolean retryable = exception instanceof ResourceAccessException
                    || (exception instanceof RestClientResponseException response
                        && (response.getStatusCode().is5xxServerError() || response.getStatusCode().value() == 429));
                if (!retryable || attempt == attempts) {
                    break;
                }
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("Notification fallback id={} type={}; appointment remains committed, notification not delivered",
            notification.id(), notification.type());
        return false;
    }
}
