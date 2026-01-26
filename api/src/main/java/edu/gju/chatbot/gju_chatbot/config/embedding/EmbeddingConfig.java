package edu.gju.chatbot.gju_chatbot.config.embedding;

import java.util.List;

import org.springframework.ai.document.DefaultContentFormatter;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.knuddels.jtokkit.api.EncodingType;

import edu.gju.chatbot.gju_chatbot.embedding.OverlapBatchingStrategy;
import edu.gju.chatbot.gju_chatbot.metadata.MetadataKeys;

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
                MetadataKeys.CHUNK_INDEX,
                MetadataKeys.FILE_ID,
                MetadataKeys.FILE_SIZE,
                MetadataKeys.FILE_NAME,
                MetadataKeys.TITLE,
                MetadataKeys.SECTION_ID,
                "parent_document_id"))
            .build(),
        MetadataMode.EMBED);
  }

  @Bean
  @ConditionalOnMissingBean(BatchingStrategy.class)
  public BatchingStrategy overlapBatchingStrategy() {
    return new OverlapBatchingStrategy();
  }
}
