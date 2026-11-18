package edu.sjsu.cmpe172.tutoringappointments;

import edu.sjsu.cmpe172.tutoringappointments.ai.*;
import edu.sjsu.cmpe172.tutoringappointments.dto.AccountDto;
import edu.sjsu.cmpe172.tutoringappointments.dto.PendingAction;
import edu.sjsu.cmpe172.tutoringappointments.service.BookingException;
import edu.sjsu.cmpe172.tutoringappointments.service.BookingService;
import edu.sjsu.cmpe172.tutoringappointments.service.SlotService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class AgentBookingTests {

    @Autowired private BookingService bookingService;
    @Autowired private SlotService slotService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void chatClientUsesMultipleToolsButDoesNotWriteUntilConfirmed() {
        long slotId = createSlot();
        AccountDto john = student("john@sjsu.edu");
        ChatSession conversation = new ChatSession(john.id());
        ChatModel model = mock(ChatModel.class);
        when(model.getOptions()).thenReturn(ToolCallingChatOptions.builder().model("test-model").build());
        String dates = "{\"startDate\":\"" + LocalDate.now() + "\",\"endDate\":\""
            + LocalDate.now().plusDays(7) + "\",\"page\":0}";
        when(model.call(any(Prompt.class))).thenReturn(
            toolCall("checkAvailability", dates),
            toolCall("bookAppointment", "{\"slotId\":" + slotId + "}"),
            text("Please review the booking and press Confirm."),
            text("You were discussing a tutoring booking."));
        ChatClient client = ChatClient.builder(model)
            .defaultAdvisors(ToolCallingAdvisor.builder().build()).build();
        AssistantService assistant = new AssistantService(client, bookingService, slotService, mock(KnowledgeService.class));

        var proposal = assistant.chat(john, conversation, "Find a slot and book it.");

        assertNotNull(proposal.pending());
        assertEquals(slotId, proposal.pending().targetId());
        assertEquals(0, activeBookings(slotId));
        assertEquals(2, conversation.history.size());
        String token = proposal.pending().confirmationId();
        assertThrows(BookingException.class, () -> assistant.confirm(john, conversation, "wrong-token"));
        assertEquals(0, activeBookings(slotId));

        assistant.confirm(john, conversation, token);
        assertEquals(1, activeBookings(slotId));
        assertThrows(BookingException.class, () -> assistant.confirm(john, conversation, token));

        assistant.chat(john, conversation, "What were we discussing?");
        verify(model, times(4)).call(any(Prompt.class));
        assertTrue(conversation.history.stream().anyMatch(message -> message.getText().contains("Find a slot")));
    }

    @Test
    void twoConfirmedAgentToolsCannotBookTheSameSlot() throws Exception {
        long slotId = createSlot();
        List<AccountDto> students = List.of(student("john@sjsu.edu"), student("student2@sjsu.edu"));
        List<ChatSession> sessions = new ArrayList<>();
        for (AccountDto student : students) {
            ChatSession session = new ChatSession(student.id());
            AppointmentTools tools = new AppointmentTools(student, session, bookingService, slotService, null, null);
            // Execute through Spring AI's real method-tool callback, not a repository mock.
            var callback = Arrays.stream(MethodToolCallbackProvider.builder().toolObjects(tools).build().getToolCallbacks())
                .filter(tool -> tool.getToolDefinition().name().equals("bookAppointment")).findFirst().orElseThrow();
            callback.call("{\"slotId\":" + slotId + "}");
            assertNotNull(session.pending);
            sessions.add(session);
        }
        assertEquals(0, activeBookings(slotId));
        AssistantService assistant = new AssistantService(null, bookingService, slotService, null);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var attempts = new ArrayList<java.util.concurrent.Future<Integer>>();
            for (int i = 0; i < 2; i++) {
                final int index = i;
                attempts.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("Barrier timeout");
                    try {
                        assistant.confirm(students.get(index), sessions.get(index), sessions.get(index).pending.confirmationId());
                        return 201;
                    } catch (BookingException exception) {
                        return exception.status();
                    }
                }));
            }
            boolean bothReady = ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            assertTrue(bothReady);
            List<Integer> outcomes = List.of(attempts.get(0).get(15, TimeUnit.SECONDS), attempts.get(1).get(15, TimeUnit.SECONDS));
            assertEquals(1, outcomes.stream().filter(code -> code == 201).count());
            assertEquals(1, outcomes.stream().filter(code -> code == 409).count());
        }
        assertEquals(1, activeBookings(slotId));
    }

    @Test
    void toolsRejectWrongRoleExpiredApprovalAndAnotherUsersConversation() {
        AccountDto john = student("john@sjsu.edu");
        ChatSession session = new ChatSession(john.id());
        assertThrows(BookingException.class, () -> new AppointmentTools(
            new AccountDto(999, "Tutor", "TUTOR"), session, bookingService, slotService, null, null));
        assertThrows(BookingException.class, () -> new AppointmentTools(
            john, new ChatSession(999), bookingService, slotService, null, null));
        session.pending = new PendingAction("expired", "BOOK", createSlot(), "Expired proposal", LocalDateTime.now().minusSeconds(1));
        var assistant = new AssistantService(null, bookingService, slotService, null);
        assertEquals(409, assertThrows(BookingException.class,
            () -> assistant.confirm(john, session, "expired")).status());
    }

    private int activeBookings(long slotId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM appointments WHERE active_slot_id = ?", Integer.class, slotId);
    }

    private AccountDto student(String email) {
        return jdbcTemplate.queryForObject("SELECT id, name, role FROM users WHERE email = ?",
            (rs, row) -> new AccountDto(rs.getLong("id"), rs.getString("name"), rs.getString("role")), email);
    }

    private long createSlot() {
        long serviceId = jdbcTemplate.queryForObject("SELECT id FROM services WHERE name = 'Calculus'", Long.class);
        long providerId = jdbcTemplate.queryForObject("SELECT provider_id FROM services WHERE id = ?", Long.class, serviceId);
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        var keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO availability_slots (provider_id, service_id, start_time, end_time, status)
                VALUES (?, ?, ?, ?, 'AVAILABLE')
                """, new String[] {"id"});
            statement.setLong(1, providerId); statement.setLong(2, serviceId);
            statement.setObject(3, start); statement.setObject(4, start.plusHours(1));
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    private ChatResponse toolCall(String name, String arguments) {
        return new ChatResponse(List.of(new Generation(AssistantMessage.builder().content("")
            .toolCalls(List.of(new AssistantMessage.ToolCall(name + "-call", "function", name, arguments))).build())));
    }

    private ChatResponse text(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
