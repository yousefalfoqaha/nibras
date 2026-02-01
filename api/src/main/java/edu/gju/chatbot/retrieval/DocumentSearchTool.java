package edu.gju.chatbot.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.exception.RagException;
import edu.gju.chatbot.exception.ToolCallingException;
import edu.gju.chatbot.metadata.DocumentAttribute.AttributeType;
import edu.gju.chatbot.metadata.DocumentMetadataRegistry;
import edu.gju.chatbot.metadata.DocumentType;
import edu.gju.chatbot.retrieval.AttributeFilter.AttributeFilterReason;
import java.io.IOException;
import java.util.Arrays;
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

    private final DocumentSearchService documentSearchService;

    private final ObjectMapper objectMapper;

    @Override
    public ToolDefinition getToolDefinition() {
        ToolDefinition definition = ToolDefinition.builder()
            .name("search_documents")
            .description(buildDescription())
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

            log.info("Tool input: {}", query.toString());

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

            List<AttributeProblem> filterProblems = collectFilterProblems(
                documentType,
                query
            );

            if (!filterProblems.isEmpty()) {
                String message = formatFilterProblemsMessage(filterProblems);
                log.info(message);
                return message;
            }

            Map<String, AttributeFilter> documentTypeAttributeFilters = query
                .getAttributeFilters()
                .entrySet()
                .stream()
                .filter(entry ->
                    documentType
                        .getRequiredAttributes()
                        .contains(entry.getKey())
                )
                .collect(
                    Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                );

            DocumentSearchQuery narrowedQuery = new DocumentSearchQuery(
                query.getQuery(),
                documentType.getName(),
                documentTypeAttributeFilters
            );

            List<Document> documents = documentSearchService.search(
                narrowedQuery
            );

            String results = documents
                .stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

            if (results.isBlank()) {
                return "No documents found.";
            }

            log.info(results);

            return results;
        } catch (IOException e) {
            throw new RagException("Failed to parse tool input: " + toolInput);
        } catch (ToolCallingException e) {
            return e.getMessage();
        }
    }

    private List<AttributeProblem> collectFilterProblems(
        DocumentType documentType,
        DocumentSearchQuery query
    ) {
        var filters = query.getAttributeFilters();
        java.util.List<AttributeProblem> problems = new java.util.ArrayList<>();

        for (String required : documentType.getRequiredAttributes()) {
            AttributeFilter f = filters == null ? null : filters.get(required);

            if (f == null || f.getValue() == null) {
                problems.add(
                    new AttributeProblem(required, ProblemType.MISSING, null)
                );
                continue;
            }

            if (f.getReason() == AttributeFilterReason.GUESS) {
                problems.add(
                    new AttributeProblem(
                        required,
                        ProblemType.GUESSED,
                        f.getValue()
                    )
                );
            }
        }

        return problems;
    }

    private String formatFilterProblemsMessage(
        List<AttributeProblem> problems
    ) {
        List<String> missing = problems
            .stream()
            .filter(p -> p.type() == ProblemType.MISSING)
            .map(AttributeProblem::name)
            .collect(Collectors.toList());

        List<String> guessed = problems
            .stream()
            .filter(p -> p.type() == ProblemType.GUESSED)
            .map(p -> {
                String value =
                    p.value() == null ? "null" : String.valueOf(p.value());
                return p.name() + "=" + value;
            })
            .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        if (!missing.isEmpty()) {
            sb
                .append("Missing required filters: ")
                .append(String.join(", ", missing))
                .append(". Please ask the user to provide them.");
        }
        if (!guessed.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb
                .append("Model provided guessed values for required filters: ")
                .append(String.join(", ", guessed))
                .append(
                    ". Please ask the user to confirm these values before searching."
                );
        }
        return sb.toString();
    }

    private static enum ProblemType {
        MISSING,
        GUESSED,
    }

    private static final record AttributeProblem(
        String name,
        ProblemType type,
        Object value
    ) {}

    private String buildInputSchema() {
        try {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "object");
            schema.put("additionalProperties", false);
            schema.put("required", List.of("query", "documentType"));

            Map<String, Object> schemaProperties = new HashMap<>();
            schemaProperties.put("query", Map.of("type", "string"));

            List<String> documentTypes = documentMetadataRegistry
                .getDocumentTypes()
                .stream()
                .map(DocumentType::getName)
                .toList();
            schemaProperties.put("documentType", Map.of("enum", documentTypes));

            Map<String, Object> attributeFilterProperties = new HashMap<>();

            documentMetadataRegistry
                .getDocumentAttributes()
                .forEach(a -> {
                    Map<String, Object> attributeFilterProperty =
                        new HashMap<>();
                    attributeFilterProperty.put("type", "object");
                    attributeFilterProperty.put(
                        "required",
                        List.of("value", "reason")
                    );

                    if (
                        a.getDescription() != null &&
                        !a.getDescription().isBlank()
                    ) {
                        attributeFilterProperty.put(
                            "description",
                            a.getDescription()
                        );
                    }

                    Map<String, Object> nestedProperties = new HashMap<>();

                    Map<String, Object> valueProperty = new HashMap<>();
                    if (a.getValues() != null && !a.getValues().isEmpty()) {
                        valueProperty.put("enum", a.getValues());
                    } else if (a.getType().equals(AttributeType.INTEGER)) {
                        valueProperty.put("type", "integer");
                    } else {
                        valueProperty.put("type", "string");
                    }
                    nestedProperties.put("value", valueProperty);

                    List<String> reasons = Arrays.stream(
                        AttributeFilterReason.values()
                    )
                        .map(Enum::name)
                        .toList();
                    nestedProperties.put("reason", Map.of("enum", reasons));

                    attributeFilterProperty.put("properties", nestedProperties);
                    attributeFilterProperties.put(
                        a.getName(),
                        attributeFilterProperty
                    );
                });

            schemaProperties.put(
                "attributeFilters",
                Map.of(
                    "type",
                    "object",
                    "properties",
                    attributeFilterProperties
                )
            );

            schema.put("properties", schemaProperties);
            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            throw new RagException("Failed to build input schema");
        }
    }

    private String buildDescription() {
        String documentTypesDescriptions = documentMetadataRegistry
            .getDocumentTypes()
            .stream()
            .map(DocumentType::toFormattedString)
            .collect(Collectors.joining("\n\n"));

        String template = """
                DOCUMENT TYPES:
                %s

                Decide the document type to search for.

                Extract all attribute filters explicitly mentioned in the conversation.

                Each attribute filter extracted has one of the following reasons:
                  - CONVERSATION (mentioned in the conversation, possibly via synonym/abbreviation)
                  - GUESS (you guessed it based on context)

                If you cannot extract an attribute, leave it blank.
            """;

        return String.format(template, documentTypesDescriptions);
    }
}
