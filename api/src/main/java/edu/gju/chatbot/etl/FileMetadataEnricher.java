package edu.gju.chatbot.etl;

import edu.gju.chatbot.exception.RagException;
import edu.gju.chatbot.metadata.DocumentAttribute;
import edu.gju.chatbot.metadata.DocumentType;
import edu.gju.chatbot.metadata.DocumentTypeRegistry;
import edu.gju.chatbot.metadata.MetadataKeys;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;

@RequiredArgsConstructor
public class FileMetadataEnricher implements Function<Document, Document> {

    private static final Logger log = LoggerFactory.getLogger(
        FileMetadataEnricher.class
    );

    private static final PromptTemplate SYSTEM_PROMPT_TEMPLATE =
        new PromptTemplate(
            """
            You are extracting useful metadata from a document for retrieval.

            - Create a clear title for the document ("title" key).
            - Extract the document type from the list below ("document_type").
            - Extract the year if mentioned ("year") as an integer, or null if no year is required for the document type.
            - Extract the required attributes ("attributes") for the selected document type, key of the attribute name, value of the attribute value.
            - You may use the metadata at the top for extra context to decide what attributes can be extracted.

            DOCUMENT TYPES:
            {document_types}

            DOCUMENT ATTRIBUTES:
            {document_attributes}
            """
        );

    private final ChatClient chatClient;

    private final DocumentTypeRegistry documentTypeRegistry;

    public Document enrich(Document document) {
        return apply(document);
    }

    @Override
    public Document apply(Document document) {
        String formattedDocumentTypes = documentTypeRegistry
            .getDocumentTypes()
            .stream()
            .map(DocumentType::toFormattedString)
            .collect(Collectors.joining("\n\n"));

        String formattedDocumentAttributes = documentTypeRegistry
            .getDocumentAttributes()
            .stream()
            .map(DocumentAttribute::toFormattedString)
            .collect(Collectors.joining("\n"));

        EnrichedMetadata enrichedMetadata = chatClient
            .prompt()
            .user(u -> u.text(document.getFormattedContent()))
            .system(s ->
                s.text(
                    SYSTEM_PROMPT_TEMPLATE.render(
                        Map.of(
                            "document_types",
                            formattedDocumentTypes,
                            "document_attributes",
                            formattedDocumentAttributes
                        )
                    )
                )
            )
            .call()
            .entity(EnrichedMetadata.class);

        log.info("Metadata inferred: {}", enrichedMetadata);

        Optional<DocumentType> optionalType =
            documentTypeRegistry.getDocumentType(
                enrichedMetadata.documentType()
            );

        if (optionalType.isEmpty()) {
            throw new RagException(
                "Unknown document type: " + enrichedMetadata.documentType()
            );
        }

        DocumentType documentType = optionalType.get();

        List<String> missingRequired =
            documentType.getMissingRequiredAttributes(
                enrichedMetadata.attributes()
            );

        if (!missingRequired.isEmpty()) {
            throw new RagException(
                String.format(
                    "Document of type '%s' is missing required attributes: [%s]",
                    documentType.getName(),
                    String.join(", ", missingRequired)
                )
            );
        }

        Map<String, Object> metadata = document.getMetadata();
        metadata.put(MetadataKeys.TITLE, enrichedMetadata.title());
        metadata.put(MetadataKeys.DOCUMENT_TYPE, documentType.getName());
        enrichedMetadata.attributes().forEach(metadata::put);

        if (documentType.isRequiresYear() && enrichedMetadata.year() == null) {
            throw new RagException(
                "Document of type '" +
                    documentType.getName() +
                    "' requires a year, but none was found."
            );
        }

        if (documentType.isRequiresYear()) {
            metadata.put("year", enrichedMetadata.year());
        }

        return document;
    }

    private record EnrichedMetadata(
        String title,
        String documentType,
        Integer year,
        Map<String, Object> attributes
    ) {}
}
