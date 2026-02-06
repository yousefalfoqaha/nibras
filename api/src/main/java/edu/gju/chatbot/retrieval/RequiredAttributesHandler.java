package edu.gju.chatbot.retrieval;

import edu.gju.chatbot.metadata.MetadataKeys;
import edu.gju.chatbot.exception.RagException;
import edu.gju.chatbot.metadata.DocumentMetadataList;
import edu.gju.chatbot.metadata.DocumentType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RequiredAttributesHandler implements SearchDecisionHandler {

  private final JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper;

  @Override
  public SearchDecisionContext handle(SearchDecisionContext context) {
    DocumentType documentType = (DocumentType) context.getMetadata().get("confirmed_document_type");
    UserQuery userQuery = context.getUserQuery();

    Map<String, Object> attributes = context.getUserQuery().getConfirmedAttributes();
    List<String> missingRequiredAttributes = documentType.getMissingRequiredAttributes(attributes);

    log.info("Processing required attributes for document type: {}", documentType.getName());
    log.info("Missing required attributes: {}", missingRequiredAttributes);

    Map<String, Object> confirmedRequiredAttributes = new HashMap<>(
        documentType
            .getValidRequiredAttributes(userQuery.getConfirmedAttributes()));

    Map<String, Object> metadataFilters = new HashMap<>(confirmedRequiredAttributes);
    metadataFilters.put(
        MetadataKeys.DOCUMENT_TYPE,
        documentType.getName());

    DocumentMetadataList candidates = fetchCandidates(metadataFilters);
    log.info("Found {} candidate documents", candidates.metadatas().size());

    if (candidates.metadatas().isEmpty()) {
      log.info("No documents found with specified required attributes: {}", confirmedRequiredAttributes);
      return context.interrupted("No documents found with specified required attributes.");
    }

    Map<String, List<Object>> unconfirmedAttributes = new HashMap<>();

    for (String a : missingRequiredAttributes) {
      List<Object> options = candidates
          .metadatas()
          .stream()
          .map(m -> m.get(a))
          .filter(Objects::nonNull)
          .distinct()
          .toList();

      if (options.size() == 1) {
        confirmedRequiredAttributes.put(a, options.get(0));
        log.info("Auto-confirmed attribute '{}' with single value: {}", a, options.get(0));
      } else {
        unconfirmedAttributes.put(a, options);
      }
    }

    if (unconfirmedAttributes.size() > 0) {
      log.info("User clarification needed for {} attributes: {}",
          unconfirmedAttributes.size(), unconfirmedAttributes.keySet());
      return context.interrupted(formatUnconfirmedAttributesMessage(unconfirmedAttributes));
    }

    log.info("All required attributes confirmed: {}", confirmedRequiredAttributes);
    return context
        .withUserQuery(userQuery.mutate().confirmedAttributes(confirmedRequiredAttributes).build())
        .withMetadata("available_documents", candidates);
  }

  @Override
  public int getOrder() {
    return 1;
  }

  private DocumentMetadataList fetchCandidates(
      Map<String, Object> metadataFilters) {
    List<Map<String, Object>> metadatas = jdbcTemplate.query(
        "SELECT metadata FROM vector_store WHERE metadata::jsonb @> ?::jsonb",
        ps -> {
          try {
            ps.setString(
                1,
                objectMapper.writeValueAsString(metadataFilters));
          } catch (JsonProcessingException e) {
            throw new RagException(
                "Failed to serialize metadataFilters",
                e);
          }
        },
        (rs, rowNum) -> {
          try {
            return objectMapper.readValue(
                rs.getString("metadata"),
                new TypeReference<Map<String, Object>>() {
                });
          } catch (Exception e) {
            throw new RagException("Failed to parse metadata", e);
          }
        });

    return new DocumentMetadataList(metadatas);
  }

  private String formatUnconfirmedAttributesMessage(
      Map<String, List<Object>> unconfirmedAttributes) {
    String distinctIssues = unconfirmedAttributes
        .entrySet()
        .stream()
        .map(entry -> {
          String attributeName = entry.getKey();
          List<Object> validOptions = entry.getValue();

          return String.format(
              " - Attribute '%s' is ambiguous. Valid options are: [%s]",
              attributeName,
              validOptions
                  .stream()
                  .map(String::valueOf)
                  .collect(Collectors.joining(", ")));
        })
        .collect(Collectors.joining("\n"));

    return ("TO CONTINUE SEARCH: Missing or ambiguous attributes.\n" +
        "You must ask the user to clarify the following attributes to continue the search:\n" +
        distinctIssues);
  }
}
