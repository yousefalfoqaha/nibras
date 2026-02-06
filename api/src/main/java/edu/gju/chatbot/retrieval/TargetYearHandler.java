package edu.gju.chatbot.retrieval;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import edu.gju.chatbot.metadata.DocumentMetadataList;
import edu.gju.chatbot.metadata.DocumentType;
import edu.gju.chatbot.metadata.MetadataKeys;
import lombok.RequiredArgsConstructor;

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

    if (availableYears.isEmpty() && documentType.isRequiresYear()) {
      return context.interrupted("No document available for any year in knowledgebase.");
    }

    UserQuery userQuery = context.getUserQuery();

    Integer latestAvailableYear = availableYears
        .stream()
        .max(Comparator.naturalOrder())
        .orElse(null);

    Integer targetYear = null;

    if (documentType.isPreferLatestYear()) {
      return context.withUserQuery(userQuery.mutate().targetYear(latestAvailableYear).build());
    }

    if (documentType.isRequiresYear() && userQuery.getTargetYear() != null) {
      Integer closestYear = availableYears
          .stream()
          .min(
              Comparator.comparingInt(y -> Math.abs(y - userQuery.getTargetYear())))
          .orElse(null);

      if (targetYear != closestYear) {
        return context.interrupted(String.format(
            "It seems there are no documents found for the exact year %s. " +
                "The closest available year is %s. Inform the user.",
            userQuery.getTargetYear(),
            closestYear));
      }

      return context;
    }

    if (documentType.isRequiresYear() && userQuery.getTargetYear() == null) {
      return context.withUserQuery(userQuery.mutate().targetYear(latestAvailableYear).build());
    }

    return context;
  }

  @Override
  public int getOrder() {
    return 3;
  }
}
