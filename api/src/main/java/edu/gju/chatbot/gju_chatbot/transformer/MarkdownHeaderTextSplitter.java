package edu.gju.chatbot.gju_chatbot.transformer;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;

public class MarkdownHeaderTextSplitter implements DocumentTransformer {

  private final ChatModel chatModel;

  public MarkdownHeaderTextSplitter(ChatModel chatModel) {
    this.chatModel = chatModel;
  }

  @Override
  public List<Document> apply(List<Document> documents) {
    String content = documents.stream()
        .map(Document::getText)
        .collect(Collectors.joining("\n"));

    return List.of();
  }
}
