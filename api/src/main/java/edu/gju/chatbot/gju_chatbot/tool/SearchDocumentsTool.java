package edu.gju.chatbot.gju_chatbot.tool;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

public class SearchDocumentsTool implements ToolCallback {

  @Override
  public ToolDefinition getToolDefinition() {
    return ToolDefinition.builder()
        .name("search_documents")
        .description("Search the document corpus for documents of various types.")
        .build();
  }

  @Override
  public String call(String toolInput) {
    throw new UnsupportedOperationException("Unimplemented method 'call'");
  }
}
