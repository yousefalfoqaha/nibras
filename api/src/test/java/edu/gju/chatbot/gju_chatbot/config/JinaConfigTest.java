package edu.gju.chatbot.gju_chatbot.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import edu.gju.chatbot.gju_chatbot.jina.JinaBatchingStrategy;
import edu.gju.chatbot.gju_chatbot.jina.JinaConnectionProperties;
import edu.gju.chatbot.gju_chatbot.jina.JinaEmbeddingModel;
import edu.gju.chatbot.gju_chatbot.jina.JinaEmbeddingProperties;

class JinaConfigTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(
          JinaConfig.class,
          RestClientAutoConfiguration.class,
          SpringAiRetryAutoConfiguration.class));

  @Test
  void propertiesAreMappedToBeans() {
    contextRunner
        .withPropertyValues(
            "spring.ai.jina.base-url=https://test.jina.ai",
            "spring.ai.jina.api-key=test-key-123",
            "spring.ai.jina.embedding.options.model=jina-embeddings-v3-test",
            "spring.ai.jina.embedding.options.late-chunking=false")
        .run(context -> {
          JinaConnectionProperties connProps = context.getBean(JinaConnectionProperties.class);
          assertThat(connProps.getBaseUrl()).isEqualTo("https://test.jina.ai");
          assertThat(connProps.getApiKey()).isEqualTo("test-key-123");

          JinaEmbeddingProperties embedProps = context.getBean(JinaEmbeddingProperties.class);
          assertThat(embedProps.getOptions().getModel()).isEqualTo("jina-embeddings-v3-test");
          assertThat(embedProps.getOptions().isLateChunking()).isFalse();

          assertThat(context).hasSingleBean(JinaEmbeddingModel.class);
          assertThat(context).hasSingleBean(JinaBatchingStrategy.class);
        });
  }

  @Test
  void shouldUseDefaultsWhenPropertiesMissing() {
    contextRunner
        .withPropertyValues("spring.ai.jina.api-key=required-key")
        .run(context -> {
          JinaEmbeddingProperties embedProps = context.getBean(JinaEmbeddingProperties.class);
          assertThat(embedProps.getOptions().getModel()).isEqualTo("jina-embeddings-v3");
          assertThat(embedProps.getOptions().isLateChunking()).isTrue();
        });
  }
}
