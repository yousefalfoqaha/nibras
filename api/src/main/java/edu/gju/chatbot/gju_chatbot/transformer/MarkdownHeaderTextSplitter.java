package edu.gju.chatbot.gju_chatbot.transformer;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;

public class MarkdownHeaderTextSplitter implements DocumentTransformer {

  @Override
  public List<Document> apply(List<Document> pages) {
    String content = pages.stream()
        .map(Document::getText)
        .collect(Collectors.joining("\n\n"));

    String[] splitContent = content.split("\n");

    return List.of();
  }
}
