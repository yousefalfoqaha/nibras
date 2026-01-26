package edu.gju.chatbot.gju_chatbot.metadata;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.gju_chatbot.exception.RagException;

public class MetadataFilterRepository {

  private ObjectMapper yamlMapper;

  private ResourceLoader resourceLoader;

  private String yamlPath;

  public MetadataFilterRepository(ObjectMapper objectMapper, ResourceLoader resourceLoader, String yamlPath) {
    this.yamlMapper = objectMapper;
    this.resourceLoader = resourceLoader;
    this.yamlPath = yamlPath;
  }

  public List<MetadataFilter> fetchMetadataFilters() {
    try {
      File yamlFile = resourceLoader.getResource("classpath:" + yamlPath).getFile();
      MetadataFilters wrapper = yamlMapper.readValue(yamlFile, MetadataFilters.class);
      return wrapper.filters();
    } catch (IOException e) {
      throw new RagException("Failed to read metadata filters: " + e.getMessage());
    } catch (NoResourceFoundException e) {
      throw new RagException("Metadata filters resource not found at: " + yamlPath);
    }
  }

  private record MetadataFilters(List<MetadataFilter> filters) {
  };
}
