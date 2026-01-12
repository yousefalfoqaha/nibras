package edu.gju.chatbot.gju_chatbot.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;

import edu.gju.chatbot.gju_chatbot.utils.DocumentMetadataKeys;

public class MarkdownTextSplitter implements Function<Document, List<Document>> {

  private static final int MAX_HEADER_DEPTH = 6;

  private final TextSplitter textSplitter;

  public MarkdownTextSplitter(TextSplitter textSplitter) {
    this.textSplitter = textSplitter;
  }

  public List<Document> split(Document markdown) {
    return apply(markdown);
  }

  @Override
  public List<Document> apply(Document document) {
    String[] lines = document.getText().split("\n");
    String[] headers = new String[MAX_HEADER_DEPTH];
    Map<String, Object> baseMetadata = document.getMetadata();
    StringBuilder sectionContent = new StringBuilder();
    List<Document> chunks = new ArrayList<>();

    for (String l : lines) {
      Header header = parseHeader(l);
      boolean newSection = header != null;

      if (newSection) {
        List<Document> sectionChunks = textSplitter.split(
            new Document(sectionContent.toString(), baseMetadata));

        for (Document chunk : sectionChunks) {
          flushChunk(chunks, chunk, headers);
        }

        headers[header.level - 1] = header.text;

        for (int i = header.level; i < MAX_HEADER_DEPTH; i++) {
          headers[i] = "";
        }

        continue;
      }

      sectionContent.append(l).append('\n');
    }

    return chunks;
  }

  private void flushChunk(List<Document> chunks, Document chunk, String[] headers) {
    if (chunk.getText().isEmpty()) {
      return;
    }

    String breadcrumbString = formatBreadcrumbs(headers);

    chunk.getMetadata().put(DocumentMetadataKeys.BREADCRUMBS, breadcrumbString);
    chunk.getMetadata().put(DocumentMetadataKeys.CHUNK_INDEX, chunks.size());

    chunks.add(chunk);
  }

  private String formatBreadcrumbs(String[] headers) {
    StringBuilder breadcrumbs = new StringBuilder();

    for (String h : headers) {
      if (h != null && !h.isEmpty()) {
        if (breadcrumbs.length() > 0) {
          breadcrumbs.append(" > ");
        }

        breadcrumbs.append(h);
      }
    }

    return breadcrumbs.toString();
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
