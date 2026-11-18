package edu.sjsu.cmpe172.tutoringappointments;

import edu.sjsu.cmpe172.tutoringappointments.ai.ReminderAgent;
import edu.sjsu.cmpe172.tutoringappointments.dto.NotificationRequest;
import edu.sjsu.cmpe172.tutoringappointments.repository.ReminderRepository;
import edu.sjsu.cmpe172.tutoringappointments.service.NotificationGateway;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReminderAgentTests {

    @Test
    void scheduledJobGeneratesAndSendsAReminder() {
        ReminderRepository repository = mock(ReminderRepository.class);
        NotificationGateway gateway = mock(NotificationGateway.class);
        ChatModel model = mock(ChatModel.class);
        when(model.getOptions()).thenReturn(ChatOptions.builder().model("test-model").build());
        when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(
            new AssistantMessage("Hi John, bring your Algebra questions!")))));
        when(repository.upcoming(isNull(), any(), any())).thenReturn(List.of(
            new ReminderRepository.Upcoming(42, "John", "john@sjsu.edu", "Alice", "Algebra", LocalDateTime.now().plusHours(2))));
        when(repository.claim(42)).thenReturn(true);
        when(repository.stillUpcoming(42)).thenReturn(true);
        when(gateway.send(any())).thenReturn(true);
        var agent = new ReminderAgent(repository, ChatClient.create(model), gateway, true);

        agent.scheduledReminders();

        verify(model).call(any(Prompt.class));
        verify(repository).saveMessage(eq(42L), contains("Hi John"));
        verify(gateway).send(argThat(item -> item.id().equals("appointment-42-REMINDER")
            && item.type().equals("REMINDER") && item.recipient().equals("john@sjsu.edu")));
        verify(repository).markSent(42);
    }

    @Test
    void failedDeliveryReusesTheSavedDraftAndDoesNotMarkSent() {
        ReminderRepository repository = mock(ReminderRepository.class);
        NotificationGateway gateway = mock(NotificationGateway.class);
        ChatClient client = mock(ChatClient.class);
        when(repository.upcoming(eq(3L), any(), any())).thenReturn(List.of(
            new ReminderRepository.Upcoming(42, "John", "john@sjsu.edu", "Alice", "Algebra", LocalDateTime.now().plusHours(2))));
        when(repository.claim(42)).thenReturn(true);
        when(repository.message(42)).thenReturn("Previously generated reminder.");
        when(repository.stillUpcoming(42)).thenReturn(true);
        when(gateway.send(any(NotificationRequest.class))).thenReturn(false);

        var result = new ReminderAgent(repository, client, gateway, false).run(3L);

        assertEquals(1, result.failed());
        verifyNoInteractions(client);
        verify(repository, never()).markSent(anyLong());
    }
}
