package edu.gju.chatbot.config.metadata;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(DocumentMetadataRegistryProperties.CONFIG_PREFIX)
public class DocumentMetadataRegistryProperties {

    public static final String CONFIG_PREFIX = "document-metadata";

    public static final String DEFAULT_YAML_PATH = "document-metadata.yaml";

    private String path = DEFAULT_YAML_PATH;
}
