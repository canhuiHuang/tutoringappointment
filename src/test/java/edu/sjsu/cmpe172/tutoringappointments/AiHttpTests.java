package edu.sjsu.cmpe172.tutoringappointments;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiHttpTests {

    @Value("${local.server.port}") private int port;

    @Test
    void aiEndpointsEnforceSessionAndRole() {
        RestClient client = RestClient.create("http://localhost:" + port);
        assertEquals(401, assertThrows(RestClientResponseException.class, () ->
            client.get().uri("/api/customer/ai/status").retrieve().toBodilessEntity()).getStatusCode().value());

        String tutorCookie = login(client, "alice@sjsu.edu");
        assertEquals(403, assertThrows(RestClientResponseException.class, () ->
            client.post().uri("/api/customer/ai/chat").header("Cookie", tutorCookie)
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Book a slot"))
                .retrieve().toBodilessEntity()).getStatusCode().value());

        String studentCookie = login(client, "john@sjsu.edu");
        assertEquals(403, assertThrows(RestClientResponseException.class, () ->
            client.post().uri("/api/provider/ai/reminders/run").header("Cookie", studentCookie)
                .retrieve().toBodilessEntity()).getStatusCode().value());
    }

    @Test
    void disabledAiExplainsSetupAndRejectsInvalidInput() {
        RestClient client = RestClient.create("http://localhost:" + port);
        String cookie = login(client, "john@sjsu.edu");
        String status = client.get().uri("/api/customer/ai/status").header("Cookie", cookie)
            .retrieve().body(String.class);
        assertTrue(status.contains("false"));
        assertEquals(503, assertThrows(RestClientResponseException.class, () ->
            client.post().uri("/api/customer/ai/chat").header("Cookie", cookie)
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Find a slot"))
                .retrieve().toBodilessEntity()).getStatusCode().value());
        assertEquals(400, assertThrows(RestClientResponseException.class, () ->
            client.post().uri("/api/customer/ai/chat").header("Cookie", cookie)
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("message", " "))
                .retrieve().toBodilessEntity()).getStatusCode().value());
    }

    private String login(RestClient client, String email) {
        var response = client.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("username", email, "password", "1234")).retrieve().toBodilessEntity();
        return response.getHeaders().getFirst("Set-Cookie").split(";")[0];
    }
}
