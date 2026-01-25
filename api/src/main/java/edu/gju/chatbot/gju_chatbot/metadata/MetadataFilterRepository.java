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
    MetadataFilterList metadataFilterList;
    try {
      File yamlFile = resourceLoader.getResource("classpath:" + yamlPath).getFile();
      metadataFilterList = yamlMapper.readValue(yamlFile, MetadataFilterList.class);
    } catch (IOException e) {
      throw new RagException("A rag exception.");
    } catch (NoResourceFoundException e) {
      throw new RagException("Resource not found.");
    }

    return metadataFilterList.filters;
  }

  private record MetadataFilterList(List<MetadataFilter> filters) {
  };
}
