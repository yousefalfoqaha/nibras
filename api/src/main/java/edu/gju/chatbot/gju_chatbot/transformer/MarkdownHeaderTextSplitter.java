package edu.gju.chatbot.gju_chatbot.transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;

import edu.gju.chatbot.gju_chatbot.utils.DocumentMetadataKeys;

public class MarkdownHeaderTextSplitter implements DocumentTransformer {

  private static final int MAX_HEADER_DEPTH = 3;

  @Override
  public List<Document> apply(List<Document> pages) {
    if (pages == null || pages.isEmpty()) {
      return List.of();
    }

    String fullText = pages.stream()
        .map(Document::getText)
        .reduce((a, b) -> a + "\n\n" + b)
        .orElse("");

    String[] lines = fullText.split("\n");

    String[] currentHeaders = new String[MAX_HEADER_DEPTH];
    StringBuilder currentChunk = new StringBuilder();
    List<Document> chunks = new ArrayList<>();

    Map<String, Object> baseMetadata = pages.get(0).getMetadata();

    for (String line : lines) {
      Header header = parseHeader(line);

      if (header != null) {
        flushChunk(chunks, currentChunk, currentHeaders, baseMetadata);

        int levelIndex = header.level - 1;
        currentHeaders[levelIndex] = header.text;

        for (int i = levelIndex + 1; i < MAX_HEADER_DEPTH; i++) {
          currentHeaders[i] = "";
        }

        continue;
      }

      currentChunk.append(line).append('\n');
    }

    flushChunk(chunks, currentChunk, currentHeaders, baseMetadata);

    return chunks;
  }

  private void flushChunk(List<Document> chunks, StringBuilder content, String[] headers,
      Map<String, Object> baseMetadata) {
    if (content.length() == 0)
      return;

    Map<String, Object> metadata = new HashMap<>(baseMetadata);

    StringBuilder breadcrumb = new StringBuilder();
    for (String h : headers) {
      if (h != null && !h.isEmpty()) {
        if (breadcrumb.length() > 0)
          breadcrumb.append(" > ");
        breadcrumb.append(h);
      }
    }

    metadata.put(DocumentMetadataKeys.BREADCRUMBS, breadcrumb.toString());
    metadata.put(DocumentMetadataKeys.CHUNK_INDEX, chunks.size());

    chunks.add(new Document(content.toString().trim(), metadata));
    content.setLength(0);
  }

  private Header parseHeader(String line) {
    int level = 0;
    int length = line.length();

    while (level < length && line.charAt(level) == '#') {
      level++;
    }

    if (level == 0 || level > MAX_HEADER_DEPTH) {
      return null;
    }

    if (level < length && line.charAt(level) != ' ') {
      return null;
    }

    String text = line.substring(level + 1).trim();
    if (text.isEmpty()) {
      return null;
    }

    return new Header(level, text);
  }

  private static final class Header {
    final int level;
    final String text;

    Header(int level, String text) {
      this.level = level;
      this.text = text;
    }
  }
}
