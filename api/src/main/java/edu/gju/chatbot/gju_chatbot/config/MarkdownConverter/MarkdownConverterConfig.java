package edu.gju.chatbot.gju_chatbot.config.MarkdownConverter;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import edu.gju.chatbot.gju_chatbot.reader.MarkdownConverter;

@Configuration
@EnableConfigurationProperties({ MarkdownConverterProperties.class })
public class MarkdownConverterConfig {

  @Bean
  public MarkdownConverter markdownConverter(MarkdownConverterProperties properties,
      RestClient.Builder restClientBuilder, RetryTemplate retryTemplate) {
    RestClient restClient = restClientBuilder
        .baseUrl(properties.getBaseUrl() + properties.getConverterPath())
        .build();

    return new MarkdownConverter(restClient, retryTemplate);
  }
}
