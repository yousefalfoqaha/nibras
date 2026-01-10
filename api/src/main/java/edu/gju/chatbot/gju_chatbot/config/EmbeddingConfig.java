package edu.gju.chatbot.gju_chatbot.config;

import java.util.List;

import org.springframework.ai.document.DefaultContentFormatter;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.knuddels.jtokkit.api.EncodingType;

import edu.gju.chatbot.gju_chatbot.utils.DocumentMetadataKeys;

@Configuration
public class EmbeddingConfig {

  @Bean
  public BatchingStrategy tokenCountBatchingStrategy() {
    return new TokenCountBatchingStrategy(
        EncodingType.CL100K_BASE,
        8191,
        0.10,
        DefaultContentFormatter.builder()
            .withExcludedEmbedMetadataKeys(List.of(
                DocumentMetadataKeys.CHUNK_INDEX,
                DocumentMetadataKeys.FILE_ID,
                DocumentMetadataKeys.FILE_SIZE,
                DocumentMetadataKeys.PAGE,
                DocumentMetadataKeys.FILE_NAME))
            .build(),
        MetadataMode.EMBED);
  }
}
