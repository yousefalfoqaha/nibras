package edu.gju.chatbot.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.exception.RagException;
import edu.gju.chatbot.metadata.DocumentAttribute;
import edu.gju.chatbot.metadata.DocumentType;
import edu.gju.chatbot.metadata.DocumentTypeRegistry;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

    private final DocumentTypeRegistry documentTypeRegistry;

    private final DocumentSearchResolver searchResolver;

    private final DocumentSearchService searchService;

    private final ObjectMapper objectMapper;

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("search_documents")
            .description(buildDescription())
            .inputSchema(
                """
                    {
                        "type": "object",
                        "properties": {
                            "query": {
                                "type": "string"
                            },
                            "documentType": {
                                "type": "string"
                            },
                            "year": {
                                "type": ["integer", "null"]
                            },
                            "documentTypeYear": {
                                "type": "string"
                            },
                            "conversationAttributes": {
                                "type": "array",
                                "items": {
                                    "type": "string"
                                }
                            },
                            "guessedAttributes": {
                                "type": "array",
                                "items": {
                                    "type": "string"
                                }
                            }
                        },
                        "required": ["query", "documentType", "year"],
                        "additionalProperties": false
                    }
                """
            )
            .build();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }

    @Override
    public String call(String toolInput) {
        log.info("=== Tool Call: search_documents ===");
        DocumentSearchIntent searchIntent;

        try {
            searchIntent = getSearchIntent(toolInput);
        } catch (RagException e) {
            return String.format(
                "Invalid tool input {}. Follow the instructions more carefully.",
                toolInput
            );
        }

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

        if (resolvedSearchIntent.getDocumentType() == null) {
            return "SEARCH ABORTED: must provide a valid document type to search for.";
        }

        if (!resolvedSearchIntent.getUnconfirmedAttributes().isEmpty()) {
            String message = formatUnconfirmedAttributesMessage(
                resolvedSearchIntent
            );
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

        return formatContextMessage(documents);
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

        Map<String, Object> confirmedAttributes = Optional.ofNullable(
            toolInput.conversationAttributes()
        )
            .map(attrs ->
                IntStream.range(0, attrs.size() / 2)
                    .boxed()
                    .collect(
                        Collectors.toMap(
                            i -> attrs.get(i * 2),
                            i -> (Object) attrs.get(i * 2 + 1)
                        )
                    )
            )
            .orElse(Collections.emptyMap());

        return new DocumentSearchIntent(
            toolInput.query(),
            toolInput.documentType(),
            toolInput.documentTypeYear(),
            confirmedAttributes,
            new HashMap<>()
        );
    }

    private String formatContextMessage(List<Document> documents) {
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

    private String formatUnconfirmedAttributesMessage(
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

    private String buildDescription() {
        String documentTypesDescriptions = documentTypeRegistry
            .getDocumentTypes()
            .stream()
            .map(DocumentType::toFormattedString)
            .collect(Collectors.joining("\n\n"));

        String attributeDescriptions = documentTypeRegistry
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
            - Pick the document type that best matches the user’s question.
            - List attributes mentioned in the conversation:
                - Put confirmed values in 'conversationAttributes' as [ATTRIBUTE_NAME, ATTRIBUTE_VALUE, ...].
                - Put inferred or uncertain values in 'guessedAttributes' as [ATTRIBUTE_NAME, ATTRIBUTE_VALUE, ...].
            - Set 'documentTypeYear' to the year mentioned in the conversation about the document type, or null if none.
            - Rewrite the query using keywords from the document type description.
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
        Integer documentTypeYear,
        List<String> conversationAttributes,
        List<String> guessedAttributes
    ) {}
}
