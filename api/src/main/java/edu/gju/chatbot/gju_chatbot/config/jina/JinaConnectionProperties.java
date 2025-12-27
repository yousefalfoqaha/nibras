package edu.gju.chatbot.gju_chatbot.config.jina;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(JinaConnectionProperties.CONFIG_PREFIX)
public class JinaConnectionProperties {

  public static final String CONFIG_PREFIX = "spring.ai.jina";

  public static final String DEFAULT_BASE_URL = "https://api.jina.ai";

  private String baseUrl = DEFAULT_BASE_URL;

  private String apiKey;
}
