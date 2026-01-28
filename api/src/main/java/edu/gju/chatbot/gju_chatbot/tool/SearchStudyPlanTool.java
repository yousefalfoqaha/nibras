package edu.gju.chatbot.gju_chatbot.tool;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

public class SearchStudyPlanTool implements ToolCallback {

  @Override
  ToolDefinition getToolDefinition() {
    return ToolDefinition.builder()
    .name("search_study_plan")
    .description("Study plan description.")
    .inputSchema(inputSchema)
  }
}
