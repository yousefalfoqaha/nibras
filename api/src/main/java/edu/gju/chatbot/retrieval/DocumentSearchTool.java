package edu.gju.chatbot.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.exception.RagException;
import edu.gju.chatbot.metadata.DocumentAttribute;
import edu.gju.chatbot.metadata.DocumentMetadataRegistry;
import edu.gju.chatbot.metadata.DocumentType;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

@RequiredArgsConstructor
public class DocumentSearchTool implements ToolCallback {

    private final DocumentMetadataRegistry documentMetadataRegistry;

    private final DocumentSearchService documentSearchService;

    private final ObjectMapper objectMapper;

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("search_documents")
            .description(buildToolDescription())
            .inputSchema(
                JsonSchemaGenerator.generateForType(DocumentSearchQuery.class)
            )
            .build();
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

        Once a document type has been selected, only include attributes found for that document type.

        IMPORTANT RULE: if a required attribute for the selected document type was not mentioned by the user, DO NOT call the tool, instead ask a clarifying question.

        Available document types:
        %s

        Available attributes:
        %s
        """.formatted(documentTypesDesc, attributesDesc);
    }
}
