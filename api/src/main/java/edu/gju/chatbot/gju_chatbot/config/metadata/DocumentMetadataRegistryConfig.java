package edu.gju.chatbot.gju_chatbot.config.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.gju.chatbot.gju_chatbot.metadata.DocumentMetadataRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
@EnableConfigurationProperties({ DocumentMetadataRegistryProperties.class })
public class DocumentMetadataRegistryConfig {

    @Bean(name = "yamlObjectMapper")
    public ObjectMapper yamlObjectMapper() {
        return new ObjectMapper(new YAMLFactory());
    }

    @Bean
    public DocumentMetadataRegistry documentMetadataRegistry(
        @Qualifier("yamlObjectMapper") ObjectMapper objectMapper,
        ResourceLoader resourceLoader,
        DocumentMetadataRegistryProperties properties
    ) {
        return new DocumentMetadataRegistry(
            objectMapper,
            resourceLoader,
            properties.getPath()
        );
    }
}
