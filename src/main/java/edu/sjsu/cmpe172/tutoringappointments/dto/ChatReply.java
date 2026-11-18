package edu.sjsu.cmpe172.tutoringappointments.dto;

import java.util.List;

public record ChatReply(
    String answer,
    PendingAction pending,
    List<KnowledgeSnippet> sources
) {}
