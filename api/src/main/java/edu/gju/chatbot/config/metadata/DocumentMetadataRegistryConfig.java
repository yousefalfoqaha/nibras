package edu.gju.chatbot.config.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.gju.chatbot.metadata.DocumentMetadataRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
@EnableConfigurationProperties({ DocumentMetadataRegistryProperties.class })
public class DocumentMetadataRegistryConfig {

    @Bean
    public DocumentMetadataRegistry documentMetadataRegistry(
        ResourceLoader resourceLoader,
        DocumentMetadataRegistryProperties properties
    ) {
        return new DocumentMetadataRegistry(
            new ObjectMapper(new YAMLFactory()),
            resourceLoader,
            properties.getPath()
        );
    }
}
