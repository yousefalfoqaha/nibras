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
    DocumentMetadataList availableDocuments = (DocumentMetadataList) context.getMetadata().get("available_documents");

    Set<Integer> availableYears = availableDocuments
        .metadatas()
        .stream()
        .map(m -> (Integer) m.get(MetadataKeys.YEAR))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    log.info("Processing target year for document type: {}", documentType.getName());
    log.info("Available years: {}", availableYears);

    if (availableYears.isEmpty() && documentType.isRequiresYear()) {
      log.info("No documents with year metadata found, but year is required");
      return context.interrupted("No document available for any year in knowledgebase.");
    }

    UserQuery userQuery = context.getUserQuery();
    Integer latestAvailableYear = availableYears
        .stream()
        .max(Comparator.naturalOrder())
        .orElse(null);

    if (documentType.isPreferLatestYear()) {
      log.info("Using latest available year: {}", latestAvailableYear);
      return context.withUserQuery(userQuery.mutate().targetYear(latestAvailableYear).build());
    }

    if (documentType.isRequiresYear() && userQuery.getTargetYear() != null) {
      Integer closestYear = availableYears
          .stream()
          .min(
              Comparator.comparingInt(y -> Math.abs(y - userQuery.getTargetYear())))
          .orElse(null);

      if (!Objects.equals(userQuery.getTargetYear(), closestYear)) {
        log.info("Requested year {} not found, closest available is {}",
            userQuery.getTargetYear(), closestYear);
        return context.interrupted(String.format(
            "It seems there are no documents found for the exact year %s. " +
                "The closest available year is %s. Inform the user.",
            userQuery.getTargetYear(),
            closestYear));
      }
      return context;
    }

    if (documentType.isRequiresYear() && userQuery.getTargetYear() == null) {
      log.info("No target year specified, using latest: {}", latestAvailableYear);
      return context.withUserQuery(userQuery.mutate().targetYear(latestAvailableYear).build());
    }

    return context;
  }

  @Override
  public int getOrder() {
    return 3;
  }
}
