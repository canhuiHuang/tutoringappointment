package edu.sjsu.cmpe172.tutoringappointments;

import edu.sjsu.cmpe172.tutoringappointments.ai.AssistantService;
import edu.sjsu.cmpe172.tutoringappointments.ai.ReminderAgent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {"ai.enabled=true", "spring.ai.google.genai.api-key=test-key-not-for-network",
    "ai.chat-model=gemini-4-hydrogen", "ai.embedding-model=gemini-embedding-001", "ai.reminders.enabled=false"})
class AiConfigurationTests {

    @Autowired private AssistantService assistantService;
    @Autowired private ReminderAgent reminderAgent;

    @Test
    void enabledAiStartsWithoutCallingGeminiDuringStartup() {
        assertNotNull(assistantService);
        assertNotNull(reminderAgent);
    }
}
