package edu.gju.chatbot.retrieval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.exception.RagException;
import edu.gju.chatbot.metadata.DocumentType;
import edu.gju.chatbot.metadata.DocumentTypeRegistry;
import edu.gju.chatbot.metadata.MetadataKeys;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DocumentSearchResolver
    implements Function<DocumentSearchIntent, DocumentSearchIntent>
{

    private static final Logger log = LoggerFactory.getLogger(
        DocumentSearchResolver.class
    );

    private final DocumentTypeRegistry documentTypeRegistry;

    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public DocumentSearchIntent apply(DocumentSearchIntent intent) {
        log.info(
            "Resolving Intent: Type='{}', Confirmed={}",
            intent.getDocumentType(),
            intent.getConfirmedAttributes()
        );

        Optional<DocumentType> documentType =
            documentTypeRegistry.getDocumentType(intent.getDocumentType());

        if (documentType.isEmpty()) {
            return new DocumentSearchIntent(
                intent.getQuery(),
                null,
                intent.getTargetYear(),
                intent.getConfirmedAttributes(),
                intent.getUnconfirmedAttributes()
            );
        }

        Map<String, Object> confirmed = new HashMap<>(
            documentType
                .get()
                .getValidRequiredAttributes(intent.getConfirmedAttributes())
        );

        Map<String, Object> metadataFilters = new HashMap<>(confirmed);
        metadataFilters.put(
            MetadataKeys.DOCUMENT_TYPE,
            documentType.get().getName()
        );

        List<Map<String, Object>> candidates = fetchCandidates(metadataFilters);

        if (candidates.isEmpty()) {
            log.warn(
                "No documents found for metadataFilters: {}",
                metadataFilters
            );
            return intent;
        }

        Map<String, List<Object>> unconfirmed = new HashMap<>();

        List<String> missingRequiredAttributes = documentType
            .get()
            .getMissingRequiredAttributes(confirmed);

        for (String a : missingRequiredAttributes) {
            List<Object> options = candidates
                .stream()
                .map(m -> m.get(a))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

            if (options.size() == 1) {
                confirmed.put(a, options.get(0));
            } else {
                unconfirmed.put(a, options);
            }
        }

        log.info(
            "Resolution Complete. Confirmed: {}, Ambiguous: {}",
            confirmed,
            unconfirmed.keySet()
        );

        if (unconfirmed.size() > 0) {
            return new DocumentSearchIntent(
                intent.getQuery(),
                documentType.get().getName(),
                intent.getTargetYear(),
                confirmed,
                unconfirmed
            );
        }

        if (
            !documentType.get().isRequiresYear() &&
            !documentType.get().isPreferLatestYear()
        ) {
            return new DocumentSearchIntent(
                intent.getQuery(),
                documentType.get().getName(),
                null,
                confirmed,
                unconfirmed
            );
        }

        Set<Integer> availableYears = candidates
            .stream()
            .map(m -> (Integer) m.get(MetadataKeys.YEAR))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (availableYears.isEmpty()) {
            return new DocumentSearchIntent(
                intent.getQuery(),
                documentType.get().getName(),
                null,
                confirmed,
                unconfirmed
            );
        }

        Integer targetYear = null;

        if (intent.getTargetYear() != null) {
            Integer closestYear = availableYears
                .stream()
                .min(
                    Comparator.comparingInt(y ->
                        Math.abs(y - intent.getTargetYear())
                    )
                )
                .orElse(null);

            targetYear = closestYear;
        }

        if (
            documentType.get().isPreferLatestYear() ||
            intent.getTargetYear() == null
        ) {
            Integer latestAvailableYear = availableYears
                .stream()
                .max(Comparator.naturalOrder())
                .orElse(null);

            targetYear = latestAvailableYear;
        }

        return new DocumentSearchIntent(
            intent.getQuery(),
            documentType.get().getName(),
            targetYear,
            confirmed,
            unconfirmed
        );
    }

    private List<Map<String, Object>> fetchCandidates(
        Map<String, Object> metadataFilters
    ) {
        return jdbcTemplate.query(
            "SELECT metadata FROM vector_store WHERE metadata::jsonb @> ?::jsonb",
            ps -> {
                try {
                    ps.setString(
                        1,
                        objectMapper.writeValueAsString(metadataFilters)
                    );
                } catch (JsonProcessingException e) {
                    throw new RagException(
                        "Failed to serialize metadataFilters",
                        e
                    );
                }
            },
            (rs, rowNum) -> {
                try {
                    return objectMapper.readValue(
                        rs.getString("metadata"),
                        new TypeReference<Map<String, Object>>() {}
                    );
                } catch (Exception e) {
                    throw new RagException("Failed to parse metadata", e);
                }
            }
        );
    }
}
