package edu.gju.chatbot.retrieval;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.gju.chatbot.metadata.MetadataKeys;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DocumentContextExpander implements DocumentTransformer {

  private static final Logger log = LoggerFactory.getLogger(
      DocumentContextExpander.class);

  private final JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper;

  @Override
  public List<Document> apply(List<Document> documents) {
    return expandChunks(documents);
  }

  private List<Document> expandChunks(List<Document> chunks) {
    List<String> sectionIds = chunks
        .stream()
        .map(doc -> doc.getMetadata().get(MetadataKeys.SECTION_ID))
        .filter(Objects::nonNull)
        .map(Object::toString)
        .distinct()
        .toList();

    if (sectionIds.isEmpty()) {
      log.warn("No section_ids found in chunks. Returning raw chunks.");
      return chunks;
    }

    log.info(
        "Identifying parent sections. Found {} unique section IDs: {}",
        sectionIds.size(),
        sectionIds);

    String inSql = sectionIds
        .stream()
        .map(s -> "'" + s + "'")
        .collect(Collectors.joining(","));

    String sql = String.format(
        """
            SELECT content, metadata
            FROM vector_store
            WHERE metadata ->> 'section_id' IN (%s)
            ORDER BY metadata ->> 'section_id', CAST(metadata ->> 'chunk_index' AS INTEGER)
            """,
        inSql);

    List<Document> expandedChunks = this.jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          try {
            Map<String, Object> meta = objectMapper.readValue(
                rs.getString("metadata"),
                new TypeReference<Map<String, Object>>() {
                });
            return new Document(rs.getString("content"), meta);
          } catch (Exception e) {
            log.error("Error parsing metadata for row {}", rowNum, e);
            return null;
          }
        });

    expandedChunks = expandedChunks
        .stream()
        .filter(Objects::nonNull)
        .toList();

    log.info(
        "Retrieved {} total chunks from database for reconstruction.",
        expandedChunks.size());

    List<Document> reconstructedDocuments = expandedChunks
        .stream()
        .collect(
            Collectors.groupingBy(d -> d.getMetadata().get(MetadataKeys.SECTION_ID).toString()))
        .values()
        .stream()
        .map(this::reconstructSection)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    log.info(
        "Reconstructed {} full section documents.",
        reconstructedDocuments.size());

    return reconstructedDocuments;
  }

  private Document reconstructSection(List<Document> sectionChunks) {
    if (sectionChunks == null || sectionChunks.isEmpty())
      return null;

    sectionChunks.sort(
        Comparator.comparingInt(doc -> Integer.parseInt(
            doc.getMetadata().get(MetadataKeys.CHUNK_INDEX).toString())));

    StringBuilder content = new StringBuilder();
    String lastBreadcrumb = "";
    Map<String, Object> metadata = sectionChunks.get(0).getMetadata();

    for (Document chunk : sectionChunks) {
      String breadcrumb = (String) chunk
          .getMetadata()
          .get(MetadataKeys.BREADCRUMBS);
      if (!Objects.equals(breadcrumb, lastBreadcrumb)) {
        content
            .append("\n** Location: ")
            .append(breadcrumb)
            .append(" **\n");
        lastBreadcrumb = breadcrumb;
      }
      content.append(chunk.getText()).append("\n");
    }

    metadata.remove(MetadataKeys.BREADCRUMBS);
    return new Document(content.toString().trim(), metadata);
  }
}
