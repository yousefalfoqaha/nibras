package edu.gju.chatbot.gju_chatbot.config.embedding;

import org.springframework.ai.model.SpringAIModelProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import edu.gju.chatbot.gju_chatbot.embedding.JinaEmbeddingModel;

@Configuration
@ConditionalOnProperty(name = SpringAIModelProperties.EMBEDDING_MODEL, havingValue = "jina", matchIfMissing = true)
@EnableConfigurationProperties({ JinaConnectionProperties.class, JinaEmbeddingProperties.class })
public class JinaEmbeddingModelConfig {

  @Bean
  public JinaEmbeddingModel jinaEmbeddingModel(
      JinaConnectionProperties connectionProperties,
      JinaEmbeddingProperties embeddingProperties,
      RestClient.Builder restClientBuilder, RetryTemplate retryTemplate) {
    RestClient restClient = restClientBuilder
        .baseUrl(connectionProperties.getBaseUrl() +
            embeddingProperties.getEmbeddingsPath())
        .defaultHeader("Authorization", "Bearer " + connectionProperties.getApiKey())
        .defaultHeader("Content-Type", "application/json")
        .build();

    return new JinaEmbeddingModel(
        restClient,
        embeddingProperties.getMetadataMode(),
        embeddingProperties.getOptions(),
        retryTemplate);
  }

}
