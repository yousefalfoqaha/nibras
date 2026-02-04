package edu.gju.chatbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.advisor.RagAdvisor;
import edu.gju.chatbot.metadata.DocumentTypeRegistry;
import edu.gju.chatbot.retrieval.DocumentSearchResolver;
import edu.gju.chatbot.retrieval.DocumentSearchService;
import edu.gju.chatbot.retrieval.DocumentSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient openAiChatClient(
        OpenAiChatModel chatModel,
        DocumentTypeRegistry documentTypeRegistry,
        DocumentSearchResolver documentSearchResolver,
        DocumentSearchService documentSearchService,
        ChatMemory chatMemory,
        ObjectMapper objectMapper
    ) {
        return ChatClient.builder(chatModel)
            .defaultToolCallbacks(
                new DocumentSearchTool(
                    documentTypeRegistry,
                    documentSearchResolver,
                    documentSearchService,
                    objectMapper
                )
            )
            .defaultAdvisors(
                new RagAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            )
            .build();
    }
}
