package edu.gju.chatbot.gju_chatbot.config;

import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

import edu.gju.chatbot.gju_chatbot.jina.JinaBatchingStrategy;
import edu.gju.chatbot.gju_chatbot.jina.JinaEmbeddingModel;
import edu.gju.chatbot.gju_chatbot.jina.JinaEmbeddingProperties;

@Configuration
public class JinaConfig {

  @Primary
  @Bean
  public JinaEmbeddingModel jinaEmbeddingModel(
      JinaEmbeddingProperties properties,
      JinaBatchingStrategy batchingStrategy,
      RestClient.Builder restClientBuilder) {
    RestClient restClient = restClientBuilder
        .baseUrl(properties.getBaseUrl() + properties.getEmbeddingsPath())
        .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
        .defaultHeader("Content-Type", "application/json")
        .build();

    return new JinaEmbeddingModel(restClient);
  }

  @Bean
  public BatchingStrategy jinaBatchingStrategy() {
    return new JinaBatchingStrategy();
  }
}
