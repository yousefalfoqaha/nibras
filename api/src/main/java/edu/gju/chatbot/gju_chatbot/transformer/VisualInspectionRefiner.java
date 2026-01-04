package edu.gju.chatbot.gju_chatbot.transformer;

import java.util.function.Function;
import java.util.List;

import org.springframework.ai.document.Document;

public class VisualInspectionRefiner implements Function<List<Document>, Document> {

  @Override
  public Document apply(List<Document> pagesWithImages) {
    return new Document("charlie kirk");
  }
}
