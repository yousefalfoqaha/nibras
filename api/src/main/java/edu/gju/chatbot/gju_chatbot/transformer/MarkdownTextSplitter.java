package edu.gju.chatbot.gju_chatbot.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;

import edu.gju.chatbot.gju_chatbot.utils.DocumentMetadataKeys;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class MarkdownTextSplitter implements Function<Document, List<Document>> {

  private static final int MAX_HEADER_DEPTH = 6;

  private final TextSplitter textSplitter;

  public MarkdownTextSplitter(TextSplitter textSplitter) {
    this.textSplitter = textSplitter;
  }

  public List<Document> split(Document markdown) {
    return this.apply(markdown);
  }

  @Override
  public List<Document> apply(Document document) {
    String[] lines = document.getText().split("\n");
    Breadcrumb[] breadcrumbs = new Breadcrumb[MAX_HEADER_DEPTH];
    Map<String, Object> baseMetadata = document.getMetadata();
    StringBuilder sectionContent = new StringBuilder();
    List<Document> chunks = new ArrayList<>();
    int sectionStartIndex = 0;

    for (String line : lines) {
      Breadcrumb newBreadcrumb = parseBreadcrumb(line);

      if (newBreadcrumb != null) {
        List<Document> sectionChunks = textSplitter.split(new Document(sectionContent.toString(), baseMetadata));

        updateBreadcrumbRanges(breadcrumbs, sectionStartIndex, sectionStartIndex + sectionChunks.size() - 1);

        for (Document chunk : sectionChunks) {
          assignBreadcrumbsToChunk(chunk, breadcrumbs, chunks.size());
          chunks.add(chunk);
        }

        clearLowerLevelBreadcrumbs(breadcrumbs, newBreadcrumb);

        sectionStartIndex = chunks.size();
        sectionContent.setLength(0);
        continue;
      }

      sectionContent.append(line).append('\n');
    }

    // Flush final section
    List<Document> sectionChunks = textSplitter.split(new Document(sectionContent.toString(), baseMetadata));
    updateBreadcrumbRanges(breadcrumbs, sectionStartIndex, sectionStartIndex + sectionChunks.size() - 1);

    for (Document chunk : sectionChunks) {
      assignBreadcrumbsToChunk(chunk, breadcrumbs, chunks.size());
      chunks.add(chunk);
    }

    return chunks;
  }

  private void assignBreadcrumbsToChunk(Document chunk, Breadcrumb[] breadcrumbs, int chunkIndex) {
    Breadcrumb[] chunkBreadcrumbs = new Breadcrumb[MAX_HEADER_DEPTH];
    for (int i = 0; i < MAX_HEADER_DEPTH; i++) {
      if (breadcrumbs[i] != null) {
        chunkBreadcrumbs[i] = breadcrumbs[i];
      }
    }

    chunk.getMetadata().put(DocumentMetadataKeys.BREADCRUMBS, chunkBreadcrumbs);
    chunk.getMetadata().put(DocumentMetadataKeys.FORMATTED_BREADCRUMBS, formatBreadcrumbs(chunkBreadcrumbs));
    chunk.getMetadata().put(DocumentMetadataKeys.CHUNK_INDEX, chunkIndex);
  }

  private void updateBreadcrumbRanges(Breadcrumb[] breadcrumbs, int startIndex, int endIndex) {
    for (int i = 0; i < MAX_HEADER_DEPTH; i++) {
      if (breadcrumbs[i] != null) {
        breadcrumbs[i].setChunkRange(new int[] { startIndex, endIndex });
      }
    }
  }

  private void clearLowerLevelBreadcrumbs(Breadcrumb[] breadcrumbs, Breadcrumb newBreadcrumb) {
    int index = newBreadcrumb.getLevel() - 1;
    breadcrumbs[index] = newBreadcrumb;

    for (int i = index + 1; i < MAX_HEADER_DEPTH; i++) {
      breadcrumbs[i] = null;
    }
  }

  private String formatBreadcrumbs(Breadcrumb[] breadcrumbs) {
    StringBuilder sb = new StringBuilder();
    for (Breadcrumb b : breadcrumbs) {
      if (b != null && !b.getText().isEmpty()) {
        if (sb.length() > 0)
          sb.append(" > ");
        sb.append(b.getText());
      }
    }
    return sb.toString();
  }

  private Breadcrumb parseBreadcrumb(String line) {
    int level = 0;
    while (level < line.length() && line.charAt(level) == '#')
      level++;

    if (level == 0 || level > MAX_HEADER_DEPTH)
      return null;
    if (line.length() <= level || line.charAt(level) != ' ')
      return null;

    String text = line.substring(level + 1).trim();
    if (text.isEmpty())
      return null;

    return new Breadcrumb(UUID.randomUUID(), text, level, new int[2]);
  }

  @AllArgsConstructor
  @Getter
  @Setter
  public static final class Breadcrumb {
    final UUID id;
    final String text;
    final int level;
    int[] chunkRange;
  }
}
