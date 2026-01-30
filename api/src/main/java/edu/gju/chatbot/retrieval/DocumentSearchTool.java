package edu.gju.chatbot.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.exception.RagException;
import edu.gju.chatbot.metadata.DocumentAttribute;
import edu.gju.chatbot.metadata.DocumentMetadataRegistry;
import edu.gju.chatbot.metadata.DocumentType;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

@RequiredArgsConstructor
public class DocumentSearchTool implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(
        DocumentSearchTool.class
    );

    private final DocumentMetadataRegistry documentMetadataRegistry;

    private final DocumentSearchService documentSearchService;

    private final ObjectMapper objectMapper;

    @Override
    public ToolDefinition getToolDefinition() {
        ToolDefinition definition = ToolDefinition.builder()
            .name("search_documents")
            .description(buildToolDescription())
            .inputSchema(
                JsonSchemaGenerator.generateForType(DocumentSearchQuery.class)
            )
            .build();

        log.info(definition.toString());

        return definition;
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }

    @Override
    public String call(String toolInput) {
        try {
            DocumentSearchQuery query = this.objectMapper.readValue(
                toolInput,
                DocumentSearchQuery.class
            );

            log.info("Tool input: {}", toolInput);

            List<Document> documents = documentSearchService.search(query);

            return documents
                .stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            throw new RagException("Failed to parse tool input: " + toolInput);
        }
    }

    private String buildToolDescription() {
        String documentTypesDesc = documentMetadataRegistry
            .getDocumentTypes()
            .stream()
            .map(DocumentType::toFormattedString)
            .collect(Collectors.joining("\n\n"));

        String attributesDesc = documentMetadataRegistry
            .getDocumentAttributes()
            .stream()
            .map(DocumentAttribute::toFormattedString)
            .collect(Collectors.joining("\n\n"));

        return """
            Search for academic documents by type and attributes.

            RULES:
            - Once a document type has been selected, ONLY include attributes found for that document type.
            - DO NOT put a default value unless a description states a default.

            DO NOT CALL THE TOOL IF:
            - A required attribute for the selected document type cannot be found, ask a clarifying question instead.

            Available document types:
            %s

            Available attributes:
            %s
        """.formatted(documentTypesDesc, attributesDesc);
    }
}
