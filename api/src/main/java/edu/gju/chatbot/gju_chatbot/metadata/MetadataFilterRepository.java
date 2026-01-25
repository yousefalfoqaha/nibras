package edu.gju.chatbot.gju_chatbot.metadata;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MetadataFilterRepository {

  @Qualifier("yamlMapper")
  private ObjectMapper yamlMapper;

  private ResourceLoader resourceLoader;

  private String yamlPath;

  public MetadataFilterRepository(ObjectMapper objectMapper, ResourceLoader resourceLoader, String yamlPath) {
    this.yamlMapper = objectMapper;
    this.resourceLoader = resourceLoader;
    this.yamlPath = yamlPath;
  }

  public List<MetadataFilter> fetchMetadataFilters() throws IOException {
    File yamlFile = resourceLoader.getResource(yamlPath).getFile();
    MetadataFilterList metadataFilterList = yamlMapper.readValue(yamlFile, MetadataFilterList.class);

    return metadataFilterList.filters;
  }

  private record MetadataFilterList(List<MetadataFilter> filters) {
  };
}
