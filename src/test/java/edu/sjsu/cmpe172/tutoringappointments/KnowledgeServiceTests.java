package edu.sjsu.cmpe172.tutoringappointments;

import edu.sjsu.cmpe172.tutoringappointments.ai.KnowledgeService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeServiceTests {

    @Test
    void policiesAreIngestedAndRelevantPassagesAreRetrieved() {
        // Deterministic test vectors exercise the real vector store without an API key.
        EmbeddingModel embedding = new EmbeddingModel() {
            private float[] vector(String text) {
                return text.toLowerCase().contains("cancel") ? new float[] {1, 0, 0} : new float[] {0, 1, 0};
            }
            @Override public float[] embed(Document document) { return vector(document.getText()); }
            @Override public EmbeddingResponse call(EmbeddingRequest request) {
                return new EmbeddingResponse(java.util.stream.IntStream.range(0, request.getInstructions().size())
                    .mapToObj(index -> new Embedding(vector(request.getInstructions().get(index)), index)).toList());
            }
            @Override public int dimensions() { return 3; }
        };
        var store = SimpleVectorStore.builder(embedding).build();
        var knowledge = new KnowledgeService(store);

        var results = knowledge.search("Can I cancel?");
        knowledge.ingest();

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(item -> item.source().contains("cancellation")
            && item.text().contains("own future appointments")));
        assertTrue(results.size() <= 3);
    }
}
