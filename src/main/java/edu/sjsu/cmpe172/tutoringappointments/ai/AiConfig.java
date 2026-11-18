package edu.sjsu.cmpe172.tutoringappointments.ai;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.embedding.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class AiConfig {

    @Bean
    public Client geminiClient(@Value("${spring.ai.google.genai.api-key:}") String apiKey) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Set GEMINI_API_KEY before enabling AI.");
        }
        return Client.builder().apiKey(apiKey).vertexAI(false)
            .httpOptions(HttpOptions.builder().timeout(20000).build()).build();
    }

    @Bean
    public ChatClient aiChatClient(Client client,
            @Value("${ai.chat-model}") String model) {
        var options = GoogleGenAiChatOptions.builder()
            .model(model).temperature(0.2).maxOutputTokens(1500).build();
        var chatModel = GoogleGenAiChatModel.builder().genAiClient(client).options(options).build();
        var manager = ToolCallingManager.builder().maxTotalToolCalls(8)
            .toolExecutionExceptionProcessor(DefaultToolExecutionExceptionProcessor.builder()
                .alwaysThrow(true).build()).build();
        return ChatClient.builder(chatModel)
            .defaultAdvisors(ToolCallingAdvisor.builder().toolCallingManager(manager).build()).build();
    }

    @Bean
    public EmbeddingModel embeddingModel(Client client,
            @Value("${ai.embedding-model}") String model) {
        var connection = GoogleGenAiEmbeddingConnectionDetails.builder().genAiClient(client).build();
        var options = GoogleGenAiTextEmbeddingOptions.builder().model(model)
            .taskType(GoogleGenAiTextEmbeddingOptions.TaskType.SEMANTIC_SIMILARITY).build();
        return new GoogleGenAiTextEmbeddingModel(connection, options);
    }

    @Bean
    public SimpleVectorStore policyVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
