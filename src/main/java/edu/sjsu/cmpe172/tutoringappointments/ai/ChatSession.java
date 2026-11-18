package edu.sjsu.cmpe172.tutoringappointments.ai;

import edu.sjsu.cmpe172.tutoringappointments.dto.KnowledgeSnippet;
import edu.sjsu.cmpe172.tutoringappointments.dto.PendingAction;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;

// Stored only in this user's HTTP session. Access is synchronized by the controller.
public class ChatSession {

    public final long userId;
    public final List<Message> history = new ArrayList<>();
    public final List<KnowledgeSnippet> sources = new ArrayList<>();
    public PendingAction pending;

    public ChatSession(long userId) {
        this.userId = userId;
    }
}
