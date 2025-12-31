package edu.gju.chatbot.gju_chatbot.config.MarkdownConverter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(MarkdownConverterProperties.CONFIG_PREFIX)
public class MarkdownConverterProperties {

  public static final String CONFIG_PREFIX = "service.markdown-converter";

  public static final String DEFAULT_BASE_URL = "http://localhost:8000";

  public static final String DEFAULT_CONVERTER_PATH = "/convert";

  private String baseUrl = DEFAULT_BASE_URL;

  private String converterPath = DEFAULT_CONVERTER_PATH;
}
