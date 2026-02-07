package edu.gju.chatbot.retrieval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.exception.RagException;
import edu.gju.chatbot.metadata.DocumentMetadataList;
import edu.gju.chatbot.metadata.DocumentType;
import edu.gju.chatbot.metadata.MetadataKeys;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@RequiredArgsConstructor
public class AttributesHandler implements SearchDecisionHandler {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public SearchDecisionContext handle(SearchDecisionContext context) {
        DocumentType documentType = (DocumentType) context
                .getMetadata()
                .get("confirmed_document_type");
        UserQuery userQuery = context.getUserQuery();

        Map<String, Object> providedAttributes = userQuery.getConfirmedAttributes();
        List<String> missingRequiredAttributes = documentType.getMissingRequiredAttributes(providedAttributes);

        log.info("Processing required attributes for document type: {}", documentType.getName());
        log.info("Missing required attributes: {}", missingRequiredAttributes);

        Map<String, Object> confirmedRequiredAttributes = new HashMap<>(
                documentType.getValidRequiredAttributes(providedAttributes));

        DocumentMetadataList candidates = fetchCandidates(documentType, confirmedRequiredAttributes);

        if (candidates.metadatas().isEmpty()) {
            return noDocumentsFoundDecision(context, confirmedRequiredAttributes);
        }

        AttributeResolutionResult resolutionResult = resolveMissingAttributes(
                missingRequiredAttributes,
                candidates,
                confirmedRequiredAttributes);

        if (resolutionResult.hasUnconfirmedAttributes()) {
            return requestUserClarificationDecision(context, resolutionResult.getUnconfirmedAttributes());
        }

        log.info("All required attributes confirmed: {}", resolutionResult.getConfirmedAttributes());

        return context
                .withUserQuery(userQuery.mutate()
                        .confirmedAttributes(resolutionResult.getConfirmedAttributes())
                        .build())
                .withMetadata("available_documents", candidates);
    }

    @Override
    public int getOrder() {
        return 1;
    }

    private SearchDecisionContext noDocumentsFoundDecision(
            SearchDecisionContext context,
            Map<String, Object> confirmedRequiredAttributes) {

        log.info("No documents found with specified required attributes: {}", confirmedRequiredAttributes);
        return context.interrupted("No documents found with specified required attributes.");
    }

    private SearchDecisionContext requestUserClarificationDecision(
            SearchDecisionContext context,
            Map<String, List<Object>> unconfirmedAttributes) {

        log.info("User clarification needed for {} attributes: {}",
                unconfirmedAttributes.size(),
                unconfirmedAttributes.keySet());

        String message = formatUnconfirmedAttributesMessage(unconfirmedAttributes);
        return context.interrupted(message);
    }

    private DocumentMetadataList fetchCandidates(
            DocumentType documentType,
            Map<String, Object> confirmedRequiredAttributes) {

        Map<String, Object> metadataFilters = new HashMap<>(confirmedRequiredAttributes);
        metadataFilters.put(MetadataKeys.DOCUMENT_TYPE, documentType.getName());

        List<Map<String, Object>> metadatas = jdbcTemplate.query(
                "SELECT metadata FROM vector_store WHERE metadata::jsonb @> ?::jsonb",
                ps -> {
                    try {
                        ps.setString(1, objectMapper.writeValueAsString(metadataFilters));
                    } catch (JsonProcessingException e) {
                        throw new RagException("Failed to serialize metadataFilters", e);
                    }
                },
                (rs, _) -> {
                    try {
                        return objectMapper.readValue(
                                rs.getString("metadata"),
                                new TypeReference<Map<String, Object>>() {
                                });
                    } catch (Exception e) {
                        throw new RagException("Failed to parse metadata", e);
                    }
                });

        DocumentMetadataList candidates = new DocumentMetadataList(metadatas);
        log.info("Found {} candidate documents", candidates.metadatas().size());

        return candidates;
    }

    private AttributeResolutionResult resolveMissingAttributes(
            List<String> missingRequiredAttributes,
            DocumentMetadataList candidates,
            Map<String, Object> confirmedRequiredAttributes) {

        Map<String, Object> allConfirmedAttributes = new HashMap<>(confirmedRequiredAttributes);
        Map<String, List<Object>> unconfirmedAttributes = new HashMap<>();

        for (String attributeName : missingRequiredAttributes) {
            List<Object> options = extractAvailableAttributeValues(candidates, attributeName);

            if (options.size() == 1) {
                allConfirmedAttributes.put(attributeName, options.get(0));
                log.info("Auto-confirmed attribute '{}' with single value: {}", attributeName, options.get(0));
            } else {
                unconfirmedAttributes.put(attributeName, options);
            }
        }

        return new AttributeResolutionResult(allConfirmedAttributes, unconfirmedAttributes);
    }

    private List<Object> extractAvailableAttributeValues(
            DocumentMetadataList candidates,
            String attributeName) {

        return candidates.metadatas()
                .stream()
                .map(m -> m.get(attributeName))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String formatUnconfirmedAttributesMessage(
            Map<String, List<Object>> unconfirmedAttributes) {

        String distinctIssues = unconfirmedAttributes.entrySet()
                .stream()
                .map(entry -> {
                    String attributeName = entry.getKey();
                    List<Object> validOptions = entry.getValue();

                    return String.format(
                            " - Attribute '%s' is ambiguous. Valid options are: [%s]",
                            attributeName,
                            validOptions.stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(", ")));
                })
                .collect(Collectors.joining("\n"));

        return ("TO CONTINUE SEARCH: Missing or ambiguous attributes.\n" +
                "You must ask the user to clarify the following attributes to continue the search:\n" +
                distinctIssues);
    }

    @Value
    private static class AttributeResolutionResult {
        Map<String, Object> confirmedAttributes;
        Map<String, List<Object>> unconfirmedAttributes;

        boolean hasUnconfirmedAttributes() {
            return !unconfirmedAttributes.isEmpty();
        }
    }
}
