package edu.gju.chatbot.retrieval;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import edu.gju.chatbot.exception.RagException;
import edu.gju.chatbot.metadata.DocumentAttribute;
import edu.gju.chatbot.metadata.DocumentType;
import edu.gju.chatbot.metadata.DocumentTypeRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.core.convert.converter.Converter;

@RequiredArgsConstructor
public class DocumentSearchTool implements ToolCallback {

    private final DocumentTypeRegistry documentTypeRegistry;

    private final SearchDecisionChain searchDecisionChain;

    private final DocumentSearchService searchService;

    private final Converter<String, UserQuery> toolInputConverter;

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("search_documents")
                .description(buildDescription())
                .inputSchema(
                        JsonSchemaGenerator.generateForType(DocumentSearchToolInput.class))
                .build();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }

    @Override
    public String call(String toolInput) {
        UserQuery userQuery;
        try {
            userQuery = this.toolInputConverter.convert(toolInput);
        } catch (RagException e) {
            return e.getMessage();
        }

        SearchDecisionResult searchDecisionResult = this.searchDecisionChain.execute(userQuery);

        if (searchDecisionResult.getInterruptMessage().isPresent()) {
            return searchDecisionResult.getInterruptMessage().get();
        }

        List<Document> retrievedDocuments = this.searchService.search(searchDecisionResult.getUserQuery());

        return retrievedDocuments.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n\n"));
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
                - List attributes (as valid JSON array of strings) mentioned in the conversation:
                    - Put confirmed values in 'conversationAttributes' as [ATTRIBUTE_NAME, ATTRIBUTE_VALUE, ...].
                    - Put inferred or uncertain values in 'guessedAttributes' as [ATTRIBUTE_NAME, ATTRIBUTE_VALUE, ...].
                - Cannot use the same attribute more than once in one tool call.
                - Set 'documentTypeYear' to the year mentioned in the conversation about the document type, or null if no year was mentioned.
                - Rewrite the query using keywords from the document type description.

                Example of correct tool input:

                {
                  "query": "remedial courses admission requirements",
                  "documentType": "study_plan",
                  "documentTypeYear": 2023,
                  "conversationAttributes": ["program", "computer science", "academic_level", "undergraduate"],
                  "guessedAttributes": []
                }
                """;

        return String.format(
                template,
                documentTypesDescriptions,
                attributeDescriptions);
    }

}
