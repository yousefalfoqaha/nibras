package edu.gju.chatbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.advisor.ChatMemoryAdvisor;
import edu.gju.chatbot.advisor.RagAdvisor;
import edu.gju.chatbot.metadata.DocumentTypeRegistry;
import edu.gju.chatbot.retrieval.DocumentSearchResolver;
import edu.gju.chatbot.retrieval.DocumentSearchService;
import edu.gju.chatbot.retrieval.DocumentSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatClientConfig {

    @Primary
    @Bean
    public ChatClient toolCallingChatClient(
        OpenAiChatModel chatModel,
        DocumentTypeRegistry documentTypeRegistry,
        DocumentSearchResolver searchResolver,
        DocumentSearchService searchService,
        ObjectMapper objectMapper,
        ChatMemory chatMemory
    ) {
        return ChatClient.builder(chatModel)
            .defaultToolCallbacks(
                new DocumentSearchTool(
                    documentTypeRegistry,
                    searchResolver,
                    searchService,
                    objectMapper
                )
            )
            .defaultAdvisors(
                new RagAdvisor(),
                ChatMemoryAdvisor.builder(chatMemory).build()
            )
            .build();
    }
}
