package edu.gju.chatbot.gju_chatbot.config.metadata;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.resource.ResourceLoader;

import edu.gju.chatbot.gju_chatbot.metadata.MetadataFilterRepository;

@Configuration
@EnableConfigurationProperties({ MetadataFilterRepositoryProperties.class })
public class MetadataFilterConfig {

  @Bean
  public ObjectMapper yamlMapper() {
    return new ObjectMapper(new YAMLFactory());
  }

  @Bean
  public MetadataFilterRepository metadataFieldYamlRepository(
      @Qualifier("yamlMapper") ObjectMapper yamlMapper,
      ResourceLoader resourceLoader,
      MetadataFilterRepositoryProperties properties) {
    return new MetadataFilterRepository(yamlMapper, resourceLoader, properties.getPath());
  }
}
