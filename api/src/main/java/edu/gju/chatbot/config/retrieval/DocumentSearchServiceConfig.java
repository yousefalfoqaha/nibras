package edu.gju.chatbot.config.retrieval;

import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.gju.chatbot.retrieval.DocumentContextExpander;
import edu.gju.chatbot.retrieval.DocumentSearchService;

@Configuration
public class DocumentSearchServiceConfig {

  @Bean
  public DocumentSearchService documentSearchService(VectorStoreRetriever retriever,
      DocumentTransformer documentContextExpander, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    DocumentTransformer documentTransformer = new DocumentContextExpander(jdbcTemplate, objectMapper);
    return new DocumentSearchService(retriever, documentTransformer);
  }
}
