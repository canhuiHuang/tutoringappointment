package edu.sjsu.cmpe172.tutoringappointments;

import com.sun.net.httpserver.HttpServer;
import edu.sjsu.cmpe172.tutoringappointments.dto.NotificationRequest;
import edu.sjsu.cmpe172.tutoringappointments.service.NotificationGateway;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NotificationGatewayTests {

    @Test
    void retriesUseTheSamePayloadAndRecoverFromTemporaryFailure() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        List<String> payloads = new ArrayList<>();
        server.createContext("/notifications", exchange -> {
            payloads.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(payloads.size() < 3 ? 503 : 204, -1);
            exchange.close();
        });
        server.start();
        try {
            NotificationGateway gateway = new NotificationGateway(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/notifications", 500, 3, 1);
            assertTrue(gateway.send(new NotificationRequest("test-BOOKED", "john@sjsu.edu", "BOOKED", "Confirmed")));
            assertEquals(3, payloads.size());
            assertEquals(1, payloads.stream().distinct().count());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void slowServiceTimesOutAndFallsBack() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/notifications", exchange -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            NotificationGateway gateway = new NotificationGateway(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/notifications", 50, 2, 1);
            assertFalse(gateway.send(new NotificationRequest("slow-BOOKED", "john@sjsu.edu", "BOOKED", "Confirmed")));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void doesNotRetryInvalidRequest() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger attempts = new AtomicInteger();
        server.createContext("/notifications", exchange -> {
            attempts.incrementAndGet();
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
        });
        server.start();
        try {
            NotificationGateway gateway = new NotificationGateway(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/notifications", 500, 3, 1);
            assertFalse(gateway.send(new NotificationRequest("invalid", "john@sjsu.edu", "BOOKED", "Confirmed")));
            assertEquals(1, attempts.get());
        } finally {
            server.stop(0);
        }
    }
}
