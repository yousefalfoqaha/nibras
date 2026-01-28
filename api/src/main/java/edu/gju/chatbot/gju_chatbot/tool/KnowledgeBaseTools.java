package edu.gju.chatbot.gju_chatbot.tool;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import edu.gju.chatbot.gju_chatbot.metadata.MetadataFilter;

public class KnowledgeBaseTools {

  private List<MetadataFilter> metadataFilters = new ArrayList<>();

  public KnowledgeBaseTools(List<MetadataFilter> metadataFilters) {
    this.metadataFilters = metadataFilters;
  }

  @Tool(description = "Study plan description")
  String searchStudyPlan(
      @ToolParam(description = "Allowed values: ") String program,
      @ToolParam(description = "Academic year (2023, 2024, etc.)") int year) {

    return "";
  }
}
