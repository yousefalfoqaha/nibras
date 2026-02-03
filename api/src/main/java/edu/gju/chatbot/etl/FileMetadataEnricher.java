package edu.gju.chatbot.etl;

import edu.gju.chatbot.exception.RagException;
import edu.gju.chatbot.metadata.DocumentAttribute;
import edu.gju.chatbot.metadata.DocumentMetadataRegistry;
import edu.gju.chatbot.metadata.DocumentMetadataValidator;
import edu.gju.chatbot.metadata.DocumentType;
import edu.gju.chatbot.metadata.MetadataKeys;
import java.util.List;
import java.util.Map;
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

    private static Logger log = LoggerFactory.getLogger(
        FileMetadataEnricher.class
    );

    private static final PromptTemplate SYSTEM_PROMPT_TEMPLATE =
        new PromptTemplate(
            """
            You are extracting useful metadata from a document for retrieval in a knowledge base.

            Create a clear, descriptive title for the provided text. It should serve as a label for the file, this title is the value for the "title" key in the structured output.

            Extract the document type ONLY from the list below, and place them as key (document_type) and value pair.

            Extract the year (if any mentioned) and place it as key (year) and value. If no year could be found of the document, place null instead.

            Extract the document attributes from the selected document type, required attributes are a must, while optional are good if they can be found. Place the attributes as key (attribute name) and value pair.

            DOCUMENT TYPES:
            <
            {document_types}
            >>>

            DOCUMENT ATTRIBUTES:
            <
            {document_attributes}
            >>>
            """
        );

    private final ChatClient chatClient;

    private final DocumentMetadataRegistry documentMetadataRegistry;

    private final DocumentMetadataValidator documentMetadataValidator;

    public Document enrich(Document document) {
        return apply(document);
    }

    @Override
    public Document apply(Document document) {
        List<DocumentType> documentTypes =
            documentMetadataRegistry.getDocumentTypes();
        String formattedDocumentTypes = documentTypes
            .stream()
            .map(DocumentType::toFormattedString)
            .collect(Collectors.joining("\n\n"));

        List<DocumentAttribute> documentAttributes =
            documentMetadataRegistry.getDocumentAttributes();
        String formattedDocumentAttributes = documentAttributes
            .stream()
            .map(DocumentAttribute::toFormattedString)
            .collect(Collectors.joining("\n\n"));

        EnrichedMetadata enrichedMetadata = this.chatClient.prompt()
            .user(u -> u.text(document.getText()))
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

        log.debug("Metadata inferred: {}", enrichedMetadata);

        documentMetadataValidator.validateDocumentAttributes(
            enrichedMetadata.attributes()
        );

        List<String> missingRequiredAttributes =
            documentMetadataValidator.getMissingDocumentTypeAttributes(
                enrichedMetadata.attributes(),
                enrichedMetadata.documentType()
            );

        if (!missingRequiredAttributes.isEmpty()) {
            throw new RagException(
                String.format(
                    "Document of type '%s' is missing required attributes: [%s].",
                    enrichedMetadata.documentType(),
                    String.join(", ", missingRequiredAttributes)
                )
            );
        }

        document
            .getMetadata()
            .put(MetadataKeys.TITLE, enrichedMetadata.title());
        document
            .getMetadata()
            .put(MetadataKeys.DOCUMENT_TYPE, enrichedMetadata.documentType());

        for (Map.Entry<String, Object> entry : enrichedMetadata
            .attributes()
            .entrySet()) {
            document.getMetadata().put(entry.getKey(), entry.getValue());
        }

        return document;
    }

    private record EnrichedMetadata(
        String title,
        String documentType,
        Map<String, Object> attributes
    ) {}
}
