package edu.gju.chatbot.config.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.gju.chatbot.metadata.DocumentTypeRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class DocumentTypeRegistryConfig {

  @Bean
  public DocumentTypeRegistry documentMetadataRegistry(ResourceLoader resourceLoader) {
    return new DocumentTypeRegistry(resourceLoader, new ObjectMapper(new YAMLFactory()));
  }
}
