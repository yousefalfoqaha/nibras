package edu.gju.chatbot.gju_chatbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.gju.chatbot.gju_chatbot.advisor.RagAdvisor;

@Configuration
public class ChatClientConfig {

  private final VectorStoreRetriever vectorStoreRetriever;

  public ChatClientConfig(VectorStoreRetriever vectorStoreRetriever) {
    this.vectorStoreRetriever = vectorStoreRetriever;
  }

  @Bean
  public ChatClient openAiChatClient(OpenAiChatModel chatModel) {
    return ChatClient.builder(chatModel)
        .defaultAdvisors(
            new RagAdvisor(vectorStoreRetriever))
        .build();
  }
}
