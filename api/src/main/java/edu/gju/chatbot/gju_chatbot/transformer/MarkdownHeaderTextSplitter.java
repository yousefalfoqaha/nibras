package edu.gju.chatbot.gju_chatbot.transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;

import edu.gju.chatbot.gju_chatbot.utils.DocumentMetadataKeys;

public class MarkdownHeaderTextSplitter implements DocumentTransformer {

  private static final Logger log = LoggerFactory.getLogger(MarkdownHeaderTextSplitter.class);
  private static final int MAX_HEADER_DEPTH = 3;

  @Override
  public List<Document> apply(List<Document> pages) {
    if (pages == null || pages.isEmpty()) {
      log.warn("MarkdownHeaderTextSplitter called with empty list.");
      return List.of();
    }

    log.info("=== Starting Markdown Splitter ({} Input Pages) ===", pages.size());

    String fullText = pages.stream()
        .map(Document::getText)
        .reduce((a, b) -> a + "\n\n" + b)
        .orElse("");

    log.debug("   > Merged Content Size: {} chars", fullText.length());

    String[] lines = fullText.split("\n");
    String[] currentHeaders = new String[MAX_HEADER_DEPTH];
    StringBuilder currentChunk = new StringBuilder();
    List<Document> chunks = new ArrayList<>();

    Map<String, Object> baseMetadata = pages.get(0).getMetadata();

    log.info("--- Processing Line-by-Line ({}) ---", lines.length);

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      Header header = parseHeader(line);

      if (header != null) {
        String indent = "   ".repeat(header.level);
        log.debug("{} [H{}] Header Found: \"{}\"", indent, header.level, header.text);

        flushChunk(chunks, currentChunk, currentHeaders, baseMetadata);

        int levelIndex = header.level - 1;
        currentHeaders[levelIndex] = header.text;

        for (int j = levelIndex + 1; j < MAX_HEADER_DEPTH; j++) {
          currentHeaders[j] = "";
        }

        log.debug("{}    -> Breadcrumbs Updated: {}", indent, formatBreadcrumbs(currentHeaders));
        continue;
      }

      currentChunk.append(line).append('\n');
    }

    // Final flush for the last section
    log.debug("   > End of text reached. Finalizing last chunk...");
    flushChunk(chunks, currentChunk, currentHeaders, baseMetadata);

    log.info("=== Markdown Split Complete. Generated {} Chunks. ===", chunks.size());
    return chunks;
  }

  private void flushChunk(List<Document> chunks, StringBuilder content, String[] headers,
      Map<String, Object> baseMetadata) {

    if (content.length() == 0) {
      return;
    }

    String contentStr = content.toString().trim();
    if (contentStr.isEmpty())
      return;

    Map<String, Object> metadata = new HashMap<>(baseMetadata);
    String breadcrumbString = formatBreadcrumbs(headers);

    metadata.put(DocumentMetadataKeys.BREADCRUMBS, breadcrumbString);
    metadata.put(DocumentMetadataKeys.CHUNK_INDEX, chunks.size());

    chunks.add(new Document(contentStr, metadata));

    log.debug("       + [CHUNK #{}] Created | Size: {} chars", chunks.size(), contentStr.length());
    log.debug("         Context: [{}]", breadcrumbString);

    content.setLength(0);
  }

  private String formatBreadcrumbs(String[] headers) {
    StringBuilder sb = new StringBuilder();
    for (String h : headers) {
      if (h != null && !h.isEmpty()) {
        if (sb.length() > 0)
          sb.append(" > ");
        sb.append(h);
      }
    }
    return sb.toString();
  }

  private Header parseHeader(String line) {
    int level = 0;
    int length = line.length();
    while (level < length && line.charAt(level) == '#') {
      level++;
    }
    if (level == 0 || level > MAX_HEADER_DEPTH)
      return null;
    if (level < length && line.charAt(level) != ' ')
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
