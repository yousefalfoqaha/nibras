package edu.gju.chatbot.gju_chatbot.jina;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(JinaEmbeddingProperties.CONFIG_PREFIX)
public class JinaEmbeddingProperties {

  public static final String CONFIG_PREFIX = "spring.ai.jina.embedding";

  public static final String DEFAULT_BASE_URL = "https://api.jina.ai";

  public static final String DEFAULT_EMBEDDING_MODEL = "jina-embeddings-v3";

  public static final String DEFAULT_EMBEDDINGS_PATH = "/v1/embeddings";

  private String baseUrl = DEFAULT_BASE_URL;

  private String apiKey;

  private String embeddingModel = DEFAULT_EMBEDDING_MODEL;

  private String embeddingsPath = DEFAULT_EMBEDDINGS_PATH;
}
