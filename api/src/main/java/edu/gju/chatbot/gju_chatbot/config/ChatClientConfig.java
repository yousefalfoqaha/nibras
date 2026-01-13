package edu.gju.chatbot.gju_chatbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.gju.chatbot.gju_chatbot.advisor.RagAdvisor;

@Configuration
public class ChatClientConfig {

  @Bean
  public ChatClient openAiChatClient(OpenAiChatModel chatModel, VectorStoreRetriever vectorStoreRetriever,
      JdbcTemplate jdbcTemplate, ObjectMapper ObjectMapper) {
    return ChatClient.builder(chatModel)
        .defaultAdvisors(
            new RagAdvisor(
                vectorStoreRetriever,
                jdbcTemplate,
                ObjectMapper))
        .build();
  }
}
