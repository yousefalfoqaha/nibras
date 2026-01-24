package edu.gju.chatbot.gju_chatbot.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;

import edu.gju.chatbot.gju_chatbot.utils.MetadataKeys;

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
    UUID currentSectionId = UUID.randomUUID();

    for (String line : lines) {
      Header header = parseHeader(line);

      if (header != null) {
        List<Document> sectionChunks = textSplitter.split(
            new Document(sectionContent.toString(), baseMetadata));

        for (Document chunk : sectionChunks) {
          flushChunk(chunks, chunk, headers, currentSectionId);
        }

        int headerIndex = header.level - 1;
        int currentTopHeaderIndex = 0;

        for (int i = 0; i < MAX_HEADER_DEPTH; i++) {
          if (headers[i] == null || headers[i].isEmpty() || headers[i].isBlank()) {
            continue;
          }

          currentTopHeaderIndex = i;
          break;
        }

        headers[headerIndex] = header.text;

        for (int i = headerIndex + 1; i < MAX_HEADER_DEPTH; i++) {
          headers[i] = "";
        }

        if (headerIndex <= currentTopHeaderIndex) {
          currentSectionId = UUID.randomUUID();
        }

        sectionContent.setLength(0);
        continue;
      }

      sectionContent.append(line).append('\n');
    }

    // flush last section
    List<Document> sectionChunks = textSplitter.split(new Document(sectionContent.toString(), baseMetadata));

    for (Document chunk : sectionChunks) {
      flushChunk(chunks, chunk, headers, currentSectionId);
    }

    return chunks;
  }

  private void flushChunk(List<Document> chunks, Document chunk, String[] headers, UUID sectionId) {
    if (chunk.getText().isEmpty()) {
      return;
    }

    String breadcrumbString = formatBreadcrumbs(headers);

    chunk.getMetadata().put(MetadataKeys.BREADCRUMBS, breadcrumbString);
    chunk.getMetadata().put(MetadataKeys.CHUNK_INDEX, chunks.size());
    chunk.getMetadata().put(MetadataKeys.SECTION_ID, sectionId);

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

    while (level < line.length() && line.charAt(level) == '#') {
      level++;
    }

    if (level == 0 || level > MAX_HEADER_DEPTH)
      return null;

    if (line.length() <= level || line.charAt(level) != ' ')
      return null;

    String text = line.substring(level + 1).trim();

    if (text.isEmpty())
      return null;

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
