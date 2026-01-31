package edu.gju.chatbot.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.gju.chatbot.exception.RagException;
import edu.gju.chatbot.exception.ToolCallingException;
import edu.gju.chatbot.metadata.DocumentAttribute.AttributeType;
import edu.gju.chatbot.metadata.DocumentMetadataRegistry;
import edu.gju.chatbot.metadata.DocumentType;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

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
            .inputSchema(buildInputSchema())
            .build();

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

            DocumentType documentType = documentMetadataRegistry
                .getDocumentTypes()
                .stream()
                .filter(t -> t.getName().equals(query.getDocumentType()))
                .findFirst()
                .orElseThrow(() ->
                    new ToolCallingException(
                        "Document type '" +
                            query.getDocumentType() +
                            "' doesn't exist or isn't an exact match with available document types."
                    )
                );

            List<String> missingAttributes = documentType
                .getRequiredAttributes()
                .stream()
                .filter(
                    attr ->
                        query.getFilters() == null ||
                        !query.getFilters().containsKey(attr)
                )
                .collect(Collectors.toList());

            if (!missingAttributes.isEmpty()) {
                throw new ToolCallingException(
                    String.format(
                        "Unable to search document type '%s'. Missing required attributes: %s. Please provide these attributes to proceed with the search.",
                        query.getDocumentType(),
                        String.join(", ", missingAttributes)
                    )
                );
            }

            List<Document> documents = documentSearchService.search(query);

            return documents
                .stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
        } catch (IOException e) {
            throw new RagException("Failed to parse tool input: " + toolInput);
        } catch (ToolCallingException e) {
            return e.getMessage();
        }
    }

    private String buildInputSchema() {
        try {
            ObjectNode schema = objectMapper.createObjectNode();
            schema.put("type", "object");

            ArrayNode required = schema.putArray("required");
            required.add("query");
            required.add("documentType");

            ObjectNode properties = schema.putObject("properties");

            properties.putObject("query").put("type", "string");

            ObjectNode documentTypeProperty = properties.putObject(
                "documentType"
            );
            ArrayNode documentTypeEnum = documentTypeProperty.putArray("enum");
            documentMetadataRegistry
                .getDocumentTypes()
                .forEach(t -> documentTypeEnum.add(t.getName()));

            ObjectNode filtersProperty = properties.putObject("filters");
            filtersProperty.put("type", "object");
            ObjectNode filterProperties = filtersProperty.putObject(
                "properties"
            );

            documentMetadataRegistry
                .getDocumentAttributes()
                .forEach(a -> {
                    ObjectNode filterSchema = filterProperties.putObject(
                        a.getName()
                    );

                    if (a.getValues() != null && !a.getValues().isEmpty()) {
                        ArrayNode enumValues = filterSchema.putArray("enum");
                        a.getValues().forEach(enumValues::add);
                    } else if (a.getType().equals(AttributeType.INTEGER)) {
                        filterSchema.put("type", "integer");
                    }

                    if (
                        a.getDescription() != null &&
                        !a.getDescription().isBlank()
                    ) {
                        filterSchema.put("description", a.getDescription());
                    }
                });

            schema.put("additionalProperties", false);

            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            throw new RagException("Failed to build input schema");
        }
    }

    private String buildToolDescription() {
        String documentTypesDesc = documentMetadataRegistry
            .getDocumentTypes()
            .stream()
            .map(DocumentType::toFormattedString)
            .collect(Collectors.joining("\n"));

        return """
            Search academic documents by type and metadata attributes.

            Document types:
            %s
        """.formatted(documentTypesDesc);
    }
}
