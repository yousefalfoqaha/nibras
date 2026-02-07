package edu.gju.chatbot.retrieval;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import edu.gju.chatbot.metadata.DocumentMetadataList;
import edu.gju.chatbot.metadata.DocumentType;
import edu.gju.chatbot.metadata.MetadataKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TargetYearHandler implements SearchDecisionHandler {

  @Override
  public SearchDecisionContext handle(SearchDecisionContext context) {
    DocumentType documentType = (DocumentType) context.getMetadata().get("confirmed_document_type");

    if (!documentType.isRequiresYear()) {
      log.info("Year not required for document type: {}", documentType.getName());
      return clearTargetYearDecision(context);
    }

    DocumentMetadataList availableDocuments = (DocumentMetadataList) context.getMetadata().get("available_documents");
    Set<Integer> availableYears = extractAvailableYears(availableDocuments);

    log.info("Processing target year for document type: {}", documentType.getName());
    log.info("Available years: {}", availableYears);

    if (availableYears.isEmpty()) {
      log.warn("No documents with year metadata found, but year is required");
      return context.interrupted("No document available for any year in knowledgebase.");
    }

    UserQuery userQuery = context.getUserQuery();
    Integer requestedYear = userQuery.getTargetYear();

    if (documentType.isPreferLatestYear()) {
      return useLatestYearDecision(context, availableYears);
    }

    if (requestedYear == null) {
      return useLatestYearDecision(context, availableYears);
    }

    return handleRequestedYearDecision(context, requestedYear, availableYears);
  }

  @Override
  public int getOrder() {
    return 3;
  }

  private SearchDecisionContext clearTargetYearDecision(SearchDecisionContext context) {
    UserQuery userQuery = context.getUserQuery();
    return context.withUserQuery(
        userQuery.mutate().targetYear(null).build());
  }

  private Set<Integer> extractAvailableYears(DocumentMetadataList availableDocuments) {
    return availableDocuments
        .metadatas()
        .stream()
        .map(m -> (Integer) m.get(MetadataKeys.YEAR))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private SearchDecisionContext useLatestYearDecision(
      SearchDecisionContext context,
      Set<Integer> availableYears) {

    Integer latestYear = availableYears
        .stream()
        .max(Comparator.naturalOrder())
        .orElseThrow(() -> new IllegalStateException("availableYears should not be empty"));

    log.info("Using latest available year: {}", latestYear);

    UserQuery userQuery = context.getUserQuery();
    return context.withUserQuery(
        userQuery.mutate().targetYear(latestYear).build());
  }

  private SearchDecisionContext handleRequestedYearDecision(
      SearchDecisionContext context,
      Integer requestedYear,
      Set<Integer> availableYears) {

    // Check if exact year is available
    if (availableYears.contains(requestedYear)) {
      log.info("Requested year {} is available", requestedYear);
      return context; // Year already set correctly
    }

    // Find closest year
    Integer closestYear = availableYears
        .stream()
        .min(Comparator.comparingInt(y -> Math.abs(y - requestedYear)))
        .orElseThrow(() -> new IllegalStateException("availableYears should not be empty"));

    log.info("Requested year {} not found, closest available is {}",
        requestedYear, closestYear);

    return context.interrupted(String.format(
        "It seems there are no documents found for the exact year %s. " +
            "The closest available year is %s. Inform the user.",
        requestedYear,
        closestYear));
  }
}
