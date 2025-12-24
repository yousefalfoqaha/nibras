package edu.gju.chatbot.gju_chatbot.config;

import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import edu.gju.chatbot.gju_chatbot.jina.JinaBatchingStrategy;
import edu.gju.chatbot.gju_chatbot.jina.JinaConnectionProperties;
import edu.gju.chatbot.gju_chatbot.jina.JinaEmbeddingModel;
import edu.gju.chatbot.gju_chatbot.jina.JinaEmbeddingProperties;

@AutoConfiguration(after = { RestClientAutoConfiguration.class, SpringAiRetryAutoConfiguration.class })
@EnableConfigurationProperties({ JinaConnectionProperties.class, JinaEmbeddingProperties.class })
@Configuration
public class JinaConfig {

  @Bean
  public JinaEmbeddingModel jinaEmbeddingModel(
      JinaConnectionProperties connectionProperties,
      JinaEmbeddingProperties embeddingProperties,
      JinaBatchingStrategy batchingStrategy,
      RestClient.Builder restClientBuilder, RetryTemplate retryTemplate) {
    RestClient restClient = restClientBuilder
        .baseUrl(connectionProperties.getBaseUrl() + embeddingProperties.getEmbeddingsPath())
        .defaultHeader("Authorization", "Bearer " + connectionProperties.getApiKey())
        .defaultHeader("Content-Type", "application/json")
        .build();

    return new JinaEmbeddingModel(
        restClient,
        embeddingProperties.getMetadataMode(),
        embeddingProperties.getOptions(),
        retryTemplate);
  }

  @Bean
  public BatchingStrategy jinaBatchingStrategy() {
    return new JinaBatchingStrategy();
  }
}
