package edu.gju.chatbot.config.retrieval;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.gju.chatbot.metadata.DocumentTypeRegistry;
import edu.gju.chatbot.retrieval.DocumentTypeHandler;
import edu.gju.chatbot.retrieval.AttributesHandler;
import edu.gju.chatbot.retrieval.SearchDecisionChain;
import edu.gju.chatbot.retrieval.SearchDecisionHandler;
import edu.gju.chatbot.retrieval.TargetYearHandler;

@Configuration
public class SearchDecisionChainConfig {

  @Bean
  public DocumentTypeHandler documentTypeHandler(
      DocumentTypeRegistry documentTypeRegistry) {
    return new DocumentTypeHandler(documentTypeRegistry);
  }

  @Bean
  public AttributesHandler attributesHandler(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper) {
    return new AttributesHandler(jdbcTemplate, objectMapper);
  }

  @Bean
  public TargetYearHandler targetYearHandler() {
    return new TargetYearHandler();
  }

  @Bean
  public SearchDecisionChain searchDecisionChain(
      List<SearchDecisionHandler> handlers) {
    return new SearchDecisionChain(handlers);
  }
}
