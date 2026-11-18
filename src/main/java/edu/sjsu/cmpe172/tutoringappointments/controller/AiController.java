package edu.sjsu.cmpe172.tutoringappointments.controller;

import edu.sjsu.cmpe172.tutoringappointments.ai.AssistantService;
import edu.sjsu.cmpe172.tutoringappointments.ai.ChatSession;
import edu.sjsu.cmpe172.tutoringappointments.ai.ReminderAgent;
import edu.sjsu.cmpe172.tutoringappointments.config.AccessConfig;
import edu.sjsu.cmpe172.tutoringappointments.dto.*;
import edu.sjsu.cmpe172.tutoringappointments.service.BookingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AiController {

    private final ObjectProvider<AssistantService> assistants;
    private final ObjectProvider<ReminderAgent> reminders;

    public AiController(ObjectProvider<AssistantService> assistants, ObjectProvider<ReminderAgent> reminders) {
        this.assistants = assistants;
        this.reminders = reminders;
    }

    @GetMapping("/api/customer/ai/status")
    public Map<String, Boolean> status() {
        return Map.of("enabled", assistants.getIfAvailable() != null);
    }

    @PostMapping("/api/customer/ai/chat")
    public ChatReply chat(HttpServletRequest request, @RequestBody AiMessageRequest input) {
        String message = validate(input.message());
        AccountDto account = AccessConfig.account(request);
        ChatSession session = conversation(request, account);
        synchronized (session) {
            return assistant().chat(account, session, message);
        }
    }

    @PostMapping("/api/customer/ai/confirm")
    public ChatReply confirm(HttpServletRequest request, @RequestBody ConfirmationRequest input) {
        AccountDto account = AccessConfig.account(request);
        ChatSession session = conversation(request, account);
        synchronized (session) {
            return assistant().confirm(account, session, input.confirmationId());
        }
    }

    @DeleteMapping("/api/customer/ai/chat")
    public void clear(HttpServletRequest request) {
        request.getSession().removeAttribute("aiConversation");
    }

    @PostMapping("/api/customer/ai/ask")
    public ChatReply ask(@RequestBody AiMessageRequest input) {
        return assistant().askPolicy(validate(input.message()));
    }

    @PostMapping("/api/provider/ai/reminders/run")
    public ReminderResult runReminders(HttpServletRequest request) {
        ReminderAgent agent = reminders.getIfAvailable();
        if (agent == null) {
            throw new BookingException(503, "AI is disabled. Set AI_ENABLED=true and GEMINI_API_KEY, then restart.");
        }
        return agent.run(AccessConfig.account(request).id());
    }

    private AssistantService assistant() {
        AssistantService service = assistants.getIfAvailable();
        if (service == null) {
            throw new BookingException(503, "AI is disabled. Set AI_ENABLED=true and GEMINI_API_KEY, then restart.");
        }
        return service;
    }

    private ChatSession conversation(HttpServletRequest request, AccountDto account) {
        var httpSession = request.getSession();
        synchronized (httpSession) {
            ChatSession session = (ChatSession) httpSession.getAttribute("aiConversation");
            if (session == null || session.userId != account.id()) {
                session = new ChatSession(account.id());
                httpSession.setAttribute("aiConversation", session);
            }
            return session;
        }
    }

    private String validate(String message) {
        if (message == null || message.isBlank() || message.length() > 2000) {
            throw new BookingException(400, "Enter a message of 1 to 2000 characters.");
        }
        return message.trim();
    }
}
