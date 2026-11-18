package edu.sjsu.cmpe172.tutoringappointments.ai;

import edu.sjsu.cmpe172.tutoringappointments.dto.KnowledgeSnippet;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class KnowledgeService {

    private final SimpleVectorStore vectorStore;
    private boolean loaded;

    public KnowledgeService(SimpleVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    // Lazy ingestion keeps startup independent of Gemini availability.
    public synchronized void ingest() {
        if (loaded) {
            return;
        }
        try {
            String content = new ClassPathResource("knowledge/tutoring-policies.md")
                .getContentAsString(StandardCharsets.UTF_8);
            List<Document> documents = new ArrayList<>();
            for (String section : content.split("(?m)^## ")) {
                if (section.isBlank()) {
                    continue;
                }
                String heading = section.lines().findFirst().orElse("Policy").trim();
                String source = "tutoring-policies.md#" + heading.toLowerCase().replaceAll("[^a-z0-9]+", "-");
                documents.add(new Document(source, section.trim(), Map.of("source", source)));
            }
            vectorStore.add(documents);
            loaded = true;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read tutoring policies.", exception);
        }
    }

    public List<KnowledgeSnippet> search(String question) {
        ingest();
        return vectorStore.similaritySearch(SearchRequest.builder()
            .query(question).topK(3).similarityThreshold(0.45).build()).stream()
            .map(document -> new KnowledgeSnippet(
                String.valueOf(document.getMetadata().get("source")), document.getText()))
            .toList();
    }
}
