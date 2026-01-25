package edu.gju.chatbot.gju_chatbot.config.metadata;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import edu.gju.chatbot.gju_chatbot.metadata.MetadataFilterRepository;

@Configuration
@EnableConfigurationProperties({ MetadataFilterRepositoryProperties.class })
public class MetadataFilterConfig {

  @Bean
  public MetadataFilterRepository metadataFilterRepository(ResourceLoader resourceLoader,
      MetadataFilterRepositoryProperties properties) {
    return new MetadataFilterRepository(new ObjectMapper(new YAMLFactory()), resourceLoader, properties.getPath());
  }
}
