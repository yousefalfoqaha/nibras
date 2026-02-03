package edu.gju.chatbot.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.exception.RagException;
import edu.gju.chatbot.metadata.DocumentAttribute;
import edu.gju.chatbot.metadata.DocumentMetadataRegistry;
import edu.gju.chatbot.metadata.DocumentType;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final DocumentSearchResolver searchResolver;

    private final DocumentSearchService searchService;

    private final ObjectMapper objectMapper;

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("search_documents")
            .description(buildDescription())
            .inputSchema(buildInputSchema())
            .build();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }

    @Override
    public String call(String toolInput) {
        log.info("=== Tool Call: search_documents ===");

        DocumentSearchIntent searchIntent = getSearchIntent(toolInput);

        log.info(
            "Initial Intent -> Query: '{}', Type: '{}', Year: {}, Confirmed: {}, Guessed: {}",
            searchIntent.getQuery(),
            searchIntent.getDocumentType(),
            searchIntent.getYear(),
            searchIntent.getConfirmedAttributes(),
            searchIntent.getUnconfirmedAttributes()
        );

        DocumentSearchIntent resolvedSearchIntent = searchResolver.apply(
            searchIntent
        );

        if (!resolvedSearchIntent.getUnconfirmedAttributes().isEmpty()) {
            String message = formatUnconfirmedAttributes(resolvedSearchIntent);
            log.warn(message);

            return message;
        }

        DocumentSearchRequest searchRequest = DocumentSearchRequest.builder()
            .query(resolvedSearchIntent.getQuery())
            .documentType(resolvedSearchIntent.getDocumentType())
            .year(resolvedSearchIntent.getYear())
            .attributes(resolvedSearchIntent.getConfirmedAttributes())
            .build();

        List<Document> documents = searchService.search(searchRequest);

        log.info("Search Service returned {} documents.", documents.size());

        return formatDocuments(documents);
    }

    private DocumentSearchIntent getSearchIntent(String rawToolInput) {
        ToolInput toolInput;
        try {
            toolInput = this.objectMapper.readValue(
                rawToolInput,
                ToolInput.class
            );
        } catch (IOException e) {
            log.error("Failed to parse tool input JSON", e);
            throw new RagException(
                "Failed to parse tool input for document searching."
            );
        }

        return new DocumentSearchIntent(
            toolInput.query(),
            toolInput.documentType(),
            toolInput.year(),
            toolInput.conversationAttributes() != null
                ? toolInput.conversationAttributes()
                : new HashMap<>(),
            new HashMap<>()
        );
    }

    private String formatDocuments(List<Document> documents) {
        String results = documents
            .stream()
            .map(Document::getFormattedContent)
            .collect(Collectors.joining("\n\n"));

        String output = results.isBlank() ? "No documents found." : results;

        String preview =
            output.length() > 1000 ? output.substring(0, 1000) + "..." : output;
        log.info("Tool Output Preview: {}", preview);

        return output;
    }

    private String formatUnconfirmedAttributes(
        DocumentSearchIntent searchIntent
    ) {
        Map<String, List<Object>> unconfirmed =
            searchIntent.getUnconfirmedAttributes();

        if (unconfirmed == null || unconfirmed.isEmpty()) {
            return "No unconfirmed attributes found.";
        }

        String distinctIssues = unconfirmed
            .entrySet()
            .stream()
            .map(entry -> {
                String attributeName = entry.getKey();
                List<Object> validOptions = entry.getValue();

                String optionsStr =
                    validOptions == null || validOptions.isEmpty()
                        ? "Unknown (User must provide this value)"
                        : validOptions
                              .stream()
                              .map(String::valueOf)
                              .collect(Collectors.joining(", "));

                return String.format(
                    " - Attribute '%s' is ambiguous. Valid options are: [%s]",
                    attributeName,
                    optionsStr
                );
            })
            .collect(Collectors.joining("\n"));

        return (
            "SEARCH ABORTED: Missing or ambiguous metadata.\n" +
            "You must ask the user to clarify the following attributes before retrying the search:\n" +
            distinctIssues
        );
    }

    private String buildInputSchema() {
        try {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "object");
            schema.put("additionalProperties", false);
            schema.put("required", List.of("query", "documentType"));

            Map<String, Object> properties = new HashMap<>();

            properties.put(
                "query",
                Map.of(
                    "type",
                    "string",
                    "description",
                    "The search query rephrased for retrieval."
                )
            );

            List<String> documentTypes = documentMetadataRegistry
                .getDocumentTypes()
                .stream()
                .map(DocumentType::getName)
                .toList();
            properties.put(
                "documentType",
                Map.of("type", "string", "enum", documentTypes)
            );

            properties.put(
                "year",
                Map.of(
                    "type",
                    "integer",
                    "description",
                    "The year of the document if explicitly mentioned. Null otherwise."
                )
            );

            Map<String, Object> attributeMapSchema = new HashMap<>();
            attributeMapSchema.put("type", "object");
            attributeMapSchema.put(
                "description",
                "Map of attribute names to values."
            );

            Map<String, Object> valueSchema = new HashMap<>();
            valueSchema.put(
                "anyOf",
                List.of(Map.of("type", "string"), Map.of("type", "integer"))
            );
            attributeMapSchema.put("additionalProperties", valueSchema);

            properties.put("conversationAttributes", attributeMapSchema);
            properties.put("guessedAttributes", attributeMapSchema);

            schema.put("properties", properties);

            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            log.error("Error building tool input schema", e);
            throw new RagException("Failed to build input schema");
        }
    }

    private String buildDescription() {
        String documentTypesDescriptions = documentMetadataRegistry
            .getDocumentTypes()
            .stream()
            .map(DocumentType::toFormattedString)
            .collect(Collectors.joining("\n\n"));

        String attributeDescriptions = documentMetadataRegistry
            .getDocumentAttributes()
            .stream()
            .map(DocumentAttribute::toFormattedString)
            .collect(Collectors.joining("\n"));

        String template = """
            DOCUMENT SEARCH TOOL

            1. DOCUMENT TYPES:
            %s

            2. AVAILABLE ATTRIBUTES:
            %s

            INSTRUCTIONS:
            - Decide the 'documentType' based on the user's intent.
            - Extract 'year' if explicitly mentioned (e.g., "2023"). If not mentioned, send null.
            - Populate 'conversationAttributes': Key-value pairs for attributes EXPLICITLY CONFIRMED by the user.
            - Populate 'guessedAttributes': Key-value pairs for attributes that are AMBIGUOUS or INFERRED (not explicitly confirmed).
            - Only use attribute keys listed in AVAILABLE ATTRIBUTES.
            - Rephrase the 'query' for search retrieval (keywords only, not a question).
            """;

        return String.format(
            template,
            documentTypesDescriptions,
            attributeDescriptions
        );
    }

    private record ToolInput(
        String query,
        String documentType,
        Integer year,
        Map<String, Object> conversationAttributes,
        Map<String, Object> guessedAttributes
    ) {}
}
