package edu.gju.chatbot.retrieval;

import java.util.Optional;

import edu.gju.chatbot.metadata.DocumentType;
import edu.gju.chatbot.metadata.DocumentTypeRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DocumentTypeValidationHandler implements SearchDecisionHandler {
  private final DocumentTypeRegistry documentTypeRegistry;

  @Override
  public SearchDecisionContext handle(SearchDecisionContext context) {
    String requestedType = context.getUserQuery().getDocumentType();
    log.info("Validating document type: {}", requestedType);

    Optional<DocumentType> documentType = documentTypeRegistry
        .getDocumentType(requestedType);

    if (documentType.isEmpty()) {
      log.info("Invalid document type requested: {}", requestedType);
      return context.interrupted("Invalid or missing document type.");
    }

    log.info("Document type validated: {}", documentType.get().getName());
    return context.withMetadata("confirmed_document_type", documentType.get());
  }

  @Override
  public int getOrder() {
    return 0;
  }
}
