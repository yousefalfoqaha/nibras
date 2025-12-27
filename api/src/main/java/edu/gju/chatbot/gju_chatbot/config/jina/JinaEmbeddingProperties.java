package edu.gju.chatbot.gju_chatbot.config.jina;

import org.springframework.ai.document.MetadataMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import edu.gju.chatbot.gju_chatbot.jina.JinaEmbeddingOptions;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
@ConfigurationProperties(JinaEmbeddingProperties.CONFIG_PREFIX)
public class JinaEmbeddingProperties {

  public static final String CONFIG_PREFIX = "spring.ai.jina.embedding";

  public static final String DEFAULT_EMBEDDINGS_PATH = "/v1/embeddings";

  public static final String DEFAULT_EMBEDDING_MODEL = "jina-embeddings-v3";

  public static final int DEFAULT_EMBEDDING_DIMENSIONS = 1024;

  public static final JinaEmbeddingOptions.Task DEFAULT_EMBEDDINGS_TASK = JinaEmbeddingOptions.Task.TEXT_MATCHING;

  public static final boolean DEFAULT_LATE_CHUNKING = true;

  private String embeddingsPath = DEFAULT_EMBEDDINGS_PATH;

  private MetadataMode metadataMode = MetadataMode.EMBED;

  @NestedConfigurationProperty
  private final JinaEmbeddingOptions options = JinaEmbeddingOptions.builder()
      .model(DEFAULT_EMBEDDING_MODEL)
      .dimensions(DEFAULT_EMBEDDING_DIMENSIONS)
      .task(DEFAULT_EMBEDDINGS_TASK)
      .lateChunking(DEFAULT_LATE_CHUNKING)
      .build();
}
