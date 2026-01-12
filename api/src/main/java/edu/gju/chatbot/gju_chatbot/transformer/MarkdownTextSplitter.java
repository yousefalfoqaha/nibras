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
    int sectionStartIndex = 0;

    for (String l : lines) {
      Header header = parseHeader(l);

      if (header != null) {
        List<Document> sectionChunks = textSplitter.split(
            new Document(sectionContent.toString(), baseMetadata));

        for (Document chunk : sectionChunks) {
          flushChunk(chunks, chunk, headers);
        }

        for (Document chunk : sectionChunks) {
          chunk.getMetadata().put(DocumentMetadataKeys.PARENT_RANGE, List.of(sectionStartIndex, chunks.size() - 1));
        }

        int headerIndex = header.level - 1;
        headers[headerIndex] = header.text;

        for (int i = headerIndex + 1; i < MAX_HEADER_DEPTH; i++) {
          headers[i] = "";
        }

        sectionStartIndex = chunks.size();
        sectionContent.setLength(0);

        continue;
      }

      sectionContent.append(l).append('\n');
    }

    // flush last section
    List<Document> sectionChunks = textSplitter.split(new Document(sectionContent.toString(), baseMetadata));

    for (Document chunk : sectionChunks) {
      flushChunk(chunks, chunk, headers);
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
