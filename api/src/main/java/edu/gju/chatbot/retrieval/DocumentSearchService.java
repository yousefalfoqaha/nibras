package edu.gju.chatbot.retrieval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.exception.RagException;
import edu.gju.chatbot.metadata.MetadataKeys;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DocumentSearchService {

    private final VectorStoreRetriever retriever;

    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    public List<Document> search(DocumentSearchQuery query) {
        List<Document> similarChunks = retriever.similaritySearch(
            SearchRequest.builder()
                .query(query.getQuery())
                .similarityThreshold(0.4)
                .topK(5)
                .build()
        );
        return expandChunks(similarChunks);
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
            return List.of(new Document("No documents found."));
        }

        String inSql = sectionIds
            .stream()
            .map(s -> "'" + s + "'")
            .collect(Collectors.joining(","));

        String queryString = String.format(
            """
            SELECT
                content,
                metadata
            FROM vector_store
            WHERE metadata ->> 'section_id' IN (%s)
            ORDER BY
                metadata ->> 'section_id',
                CAST(metadata ->> 'chunk_index' AS INTEGER)
            """,
            inSql
        );

        List<Document> expandedChunks = this.jdbcTemplate.query(
            queryString,
            (resultSet, rowNum) -> {
                Map<String, Object> metadata;
                try {
                    metadata = objectMapper.readValue(
                        resultSet.getString("metadata"),
                        new TypeReference<Map<String, Object>>() {}
                    );
                } catch (Exception e) {
                    throw new RagException("Failed to parse document metadata");
                }
                return new Document(resultSet.getString("content"), metadata);
            }
        );

        Map<String, List<Document>> sectionMap = expandedChunks
            .stream()
            .collect(
                Collectors.groupingBy(doc ->
                    doc.getMetadata().get(MetadataKeys.SECTION_ID).toString()
                )
            );

        return sectionIds
            .stream()
            .map(sectionId ->
                reconstructSection(sectionId, sectionMap.get(sectionId))
            )
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private Document reconstructSection(
        String sectionId,
        List<Document> sectionChunks
    ) {
        if (sectionChunks == null || sectionChunks.isEmpty()) {
            return null;
        }

        sectionChunks.sort(
            Comparator.comparingInt(doc ->
                Integer.parseInt(
                    doc.getMetadata().get(MetadataKeys.CHUNK_INDEX).toString()
                )
            )
        );

        StringBuilder sectionContent = new StringBuilder();
        String lastBreadcrumb = "";

        Map<String, Object> firstChunkMetadata = sectionChunks
            .get(0)
            .getMetadata();
        String title = (String) firstChunkMetadata.get(MetadataKeys.TITLE);
        String fileId = (String) firstChunkMetadata.get(MetadataKeys.FILE_ID);

        sectionContent
            .append("=== TITLE ===\n")
            .append(title)
            .append("\n====================\n\n");

        for (Document chunk : sectionChunks) {
            String currentBreadcrumb = (String) chunk
                .getMetadata()
                .get(MetadataKeys.BREADCRUMBS);

            if (!Objects.equals(currentBreadcrumb, lastBreadcrumb)) {
                sectionContent
                    .append("** Location: ")
                    .append(currentBreadcrumb)
                    .append(" **\n");
                lastBreadcrumb = currentBreadcrumb;
            }

            sectionContent.append(chunk.getText()).append("\n");
        }

        Map<String, Object> sectionMetadata = new HashMap<>();
        sectionMetadata.put(MetadataKeys.SECTION_ID, sectionId);
        sectionMetadata.put(MetadataKeys.FILE_ID, fileId);
        sectionMetadata.put(MetadataKeys.TITLE, title);

        return new Document(sectionContent.toString().trim(), sectionMetadata);
    }
}
