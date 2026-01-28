package edu.gju.chatbot.gju_chatbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.gju_chatbot.advisor.RagAdvisor;
import edu.gju.chatbot.gju_chatbot.metadata.DocumentMetadataRegistry;
import edu.gju.chatbot.gju_chatbot.tool.SearchDocumentsTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient openAiChatClient(
        OpenAiChatModel chatModel,
        DocumentMetadataRegistry documentMetadataRegistry,
        VectorStoreRetriever vectorStoreRetriever,
        JdbcTemplate jdbcTemplate,
        ObjectMapper objectMapper
    ) {
        return ChatClient.builder(chatModel)
            .defaultToolCallbacks(
                new SearchDocumentsTool(documentMetadataRegistry)
            )
            .defaultAdvisors(
                new RagAdvisor(vectorStoreRetriever, jdbcTemplate, objectMapper)
            )
            .build();
    }
}
