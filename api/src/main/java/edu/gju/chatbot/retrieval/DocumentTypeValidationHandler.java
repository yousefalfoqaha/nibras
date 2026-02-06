package edu.gju.chatbot.retrieval;

import java.util.Optional;

import edu.gju.chatbot.metadata.DocumentType;
import edu.gju.chatbot.metadata.DocumentTypeRegistry;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DocumentTypeValidationHandler implements SearchDecisionHandler {

  private final DocumentTypeRegistry documentTypeRegistry;

  @Override
  public SearchDecisionContext handle(SearchDecisionContext context) {
    Optional<DocumentType> documentType = documentTypeRegistry
        .getDocumentType(context.getUserQuery().getDocumentType());

    if (documentType.isEmpty()) {
      return context.interrupted("Invalid or missing document type.");
    }

    return context.withMetadata("confirmed_document_type", documentType);
  }

  @Override
  public int getOrder() {
    return 0;
  }
}
