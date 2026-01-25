package edu.gju.chatbot.gju_chatbot.config.metadata;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(MetadataFilterRepositoryProperties.CONFIG_PREFIX)
public class MetadataFilterRepositoryProperties {

  public static final String CONFIG_PREFIX = "metadata-filters";

  public static final String DEFAULT_YAML_PATH = "classpath:metadata-filters.yml";

  private String path = DEFAULT_YAML_PATH;
}
