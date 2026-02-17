package edu.gju.chatbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.advisor.ChatMemoryAdvisor;
import edu.gju.chatbot.advisor.RagAdvisor;
import edu.gju.chatbot.metadata.DocumentTypeRegistry;
import edu.gju.chatbot.retrieval.DocumentSearchService;
import edu.gju.chatbot.retrieval.DocumentSearchTool;
import edu.gju.chatbot.retrieval.DocumentSearchToolInputConverter;
import edu.gju.chatbot.retrieval.SearchDecisionChain;
import edu.gju.chatbot.retrieval.UserQuery;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;

@Configuration
public class ChatClientConfig {

  @Bean
  public DocumentSearchToolInputConverter documentSearchToolInputConverter(
      ObjectMapper ObjectMapper) {
    return new DocumentSearchToolInputConverter(ObjectMapper);
  }

  @Primary
  @Bean
  public ChatClient questionAnswerChatClient(
      OpenAiChatModel chatModel,
      DocumentTypeRegistry documentTypeRegistry,
      Converter<String, UserQuery> documentSearchToolInputConverter,
      SearchDecisionChain searchDecisionChain,
      DocumentSearchService searchService,
      ChatMemory chatMemory) {
    return ChatClient.builder(chatModel)
        .defaultToolCallbacks(
            new DocumentSearchTool(
                documentTypeRegistry,
                searchDecisionChain,
                searchService,
                documentSearchToolInputConverter))
        .defaultAdvisors(new RagAdvisor(), ChatMemoryAdvisor.builder(chatMemory).build())
        .build();
  }
}
