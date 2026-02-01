package edu.gju.chatbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.advisor.RagAdvisor;
import edu.gju.chatbot.metadata.DocumentMetadataRegistry;
import edu.gju.chatbot.retrieval.DocumentSearchService;
import edu.gju.chatbot.retrieval.DocumentSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient openAiChatClient(
        OpenAiChatModel chatModel,
        DocumentMetadataRegistry documentMetadataRegistry,
        DocumentSearchService documentSearchService,
        ObjectMapper objectMapper
    ) {
        return ChatClient.builder(chatModel)
            .defaultToolCallbacks(
                new DocumentSearchTool(
                    documentMetadataRegistry,
                    documentSearchService,
                    objectMapper
                )
            )
            .defaultAdvisors(new RagAdvisor())
            .build();
    }
}
