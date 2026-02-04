package edu.gju.chatbot.retrieval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.metadata.DocumentType;
import edu.gju.chatbot.metadata.DocumentTypeRegistry;
import edu.gju.chatbot.metadata.MetadataKeys;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DocumentSearchService {

    private static final Logger log = LoggerFactory.getLogger(
        DocumentSearchService.class
    );

    private final DocumentTypeRegistry documentTypeRegistry;

    private final VectorStoreRetriever retriever;

    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    public List<Document> search(DocumentSearchRequest searchRequest) {
        Optional<DocumentType> documentType =
            documentTypeRegistry.getDocumentType(
                searchRequest.getDocumentType()
            );

        log.info("=== Starting Search ===");
        log.info(
            "Query: '{}', Type: '{}', Year: {}",
            searchRequest.getQuery(),
            searchRequest.getDocumentType(),
            searchRequest.getYear()
        );

        if (documentType.isEmpty()) {
            return List.of();
        }

        if (searchRequest.getYear() != null) {
            List<Document> documents = doSearch(searchRequest);

            log.info(
                "Explicit year search found {} documents.",
                documents.size()
            );

            return documents;
        }

        if (!documentType.get().isRequiresYear()) {
            List<Document> documents = doSearch(
                DocumentSearchRequest.builder()
                    .query(searchRequest.getQuery())
                    .documentType(searchRequest.getDocumentType())
                    .year(null)
                    .attributes(searchRequest.getAttributes())
                    .build()
            );

            log.info(
                "Searched documents without year, found {} documents.",
                documents.size()
            );

            return documents;
        }

        int latestYear = Year.now().getValue();
        log.info(
            "No year provided. Attempting fallback search starting from {}.",
            latestYear
        );

        for (int i = 0; i < 5; i++) {
            int yearToTry = latestYear - i;
            log.info(">>> Trying search for year: {}", yearToTry);

            List<Document> documents = doSearch(
                DocumentSearchRequest.builder()
                    .query(searchRequest.getQuery())
                    .documentType(searchRequest.getDocumentType())
                    .year(yearToTry)
                    .attributes(searchRequest.getAttributes())
                    .build()
            );

            if (!documents.isEmpty()) {
                log.info(
                    "Match found in year {}. Returning {} documents.",
                    yearToTry,
                    documents.size()
                );
                return documents;
            }
        }

        log.info("Fallback search exhausted. No documents found.");
        return List.of();
    }

    private List<Document> doSearch(DocumentSearchRequest searchRequest) {
        String filter = buildFilterExpression(searchRequest);
        log.info("Applied Filter: [{}]", filter != null ? filter : "NONE");

        List<Document> similarChunks = retriever.similaritySearch(
            SearchRequest.builder()
                .query(searchRequest.getQuery())
                .similarityThreshold(0.4)
                .filterExpression(filter)
                .topK(10)
                .build()
        );

        log.info("Vector Store returned {} raw chunks.", similarChunks.size());

        if (similarChunks.isEmpty()) {
            return List.of();
        }

        return expandChunks(similarChunks);
    }

    private String buildFilterExpression(DocumentSearchRequest searchRequest) {
        List<String> filterParts = new ArrayList<>();

        if (searchRequest.getAttributes() != null) {
            for (Map.Entry<String, Object> entry : searchRequest
                .getAttributes()
                .entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                String condition = (val instanceof String)
                    ? String.format("%s == '%s'", key, val)
                    : String.format("%s == %s", key, val);

                filterParts.add(condition);
            }
        }

        if (
            searchRequest.getDocumentType() != null &&
            !searchRequest.getDocumentType().isBlank()
        ) {
            filterParts.add(
                "document_type == '" + searchRequest.getDocumentType() + "'"
            );
        }

        if (searchRequest.getYear() != null) {
            filterParts.add("year == " + searchRequest.getYear());
        }

        return filterParts.isEmpty() ? null : String.join(" && ", filterParts);
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
            sectionIds
        );

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
            inSql
        );

        List<Document> expandedChunks = this.jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                try {
                    Map<String, Object> meta = objectMapper.readValue(
                        rs.getString("metadata"),
                        new TypeReference<Map<String, Object>>() {}
                    );
                    return new Document(rs.getString("content"), meta);
                } catch (Exception e) {
                    log.error("Error parsing metadata for row {}", rowNum, e);
                    return null;
                }
            }
        );

        expandedChunks = expandedChunks
            .stream()
            .filter(Objects::nonNull)
            .toList();

        log.info(
            "Retrieved {} total chunks from database for reconstruction.",
            expandedChunks.size()
        );

        List<Document> reconstructedDocuments = expandedChunks
            .stream()
            .collect(
                Collectors.groupingBy(d ->
                    d.getMetadata().get(MetadataKeys.SECTION_ID).toString()
                )
            )
            .values()
            .stream()
            .map(this::reconstructSection)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        log.info(
            "Reconstructed {} full section documents.",
            reconstructedDocuments.size()
        );

        return reconstructedDocuments;
    }

    private Document reconstructSection(List<Document> sectionChunks) {
        if (sectionChunks == null || sectionChunks.isEmpty()) return null;

        sectionChunks.sort(
            Comparator.comparingInt(doc ->
                Integer.parseInt(
                    doc.getMetadata().get(MetadataKeys.CHUNK_INDEX).toString()
                )
            )
        );

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
